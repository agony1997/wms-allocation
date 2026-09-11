package com.agony.wmsallocation.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 守門測試：清點 controller package 下每一支端點都掛載了 {@link RequireRole}。
 *
 * <p><b>白名單採明列</b>：沒掛標註的端點集合必須「等於」{@link #UNPROTECTED}，
 * 而不是「沒標註就跳過」。
 * 因此登入端點改路徑、或哪天反而被掛上標註，這支測試都會紅，需要有意識地更新白名單。
 *
 * <p>只清點「有沒有掛」，不比對角色內容。
 */
class RequireRoleCoverageTest {

    private static final String CONTROLLER_PACKAGE = "com.agony.wmsallocation.controller";

    /** 排除端點 */
    private static final Set<String> UNPROTECTED = Set.of("POST /api/auth/login");

    @Test
    void everyEndpointShouldDeclareRequireRole() {
        Set<String> unannotatedEndpoints = new TreeSet<>();

        for (Class<?> controller : scanControllers()) {
            String prefix = pathPrefixOf(controller);
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (method.isSynthetic() || mapping == null) {
                    continue; // 不是端點方法
                }
                String endpoint = describe(mapping, prefix);
                if (!method.isAnnotationPresent(RequireRole.class)) {
                    unannotatedEndpoints.add(endpoint);
                }
            }
        }

        assertThat(unannotatedEndpoints)
                .as("端點未掛 @RequireRole")
                .containsExactlyInAnyOrderElementsOf(UNPROTECTED);
    }

    private Set<Class<?>> scanControllers() {
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<Class<?>> controllers = new LinkedHashSet<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(CONTROLLER_PACKAGE)) {
            String className = definition.getBeanClassName();
            try {
                controllers.add(Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new AssertionError("掃到了類別卻載不進來：" + className, e);
            }
        }
        return controllers;
    }

    private String pathPrefixOf(Class<?> controller) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
        return (mapping == null || mapping.path().length == 0) ? "" : mapping.path()[0];
    }

    /**
     * {@code @GetMapping} 等六種標註都是 {@code @RequestMapping} 的別名，
     * findMergedAnnotation 會把它們攤平成同一種，不必逐種判斷。
     */
    private String describe(RequestMapping mapping, String prefix) {
        RequestMethod[] verbs = mapping.method();
        String verb = (verbs.length == 0) ? "ANY" : verbs[0].name();
        String path = (mapping.path().length == 0) ? "" : mapping.path()[0];
        return verb + " " + prefix + path;
    }
}
