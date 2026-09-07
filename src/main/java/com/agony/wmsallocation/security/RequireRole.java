package com.agony.wmsallocation.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 自訂標註：標在 Controller 的 API 方法上，宣告「呼叫這支 API 需要什麼角色」。
// 由 JwtInterceptor 讀取並執行檢查，角色不符就回 403。相當於 Spring Security 的 @PreAuthorize。

// 慣例：Controller 的每一支端點都必須掛這個標註，未標註一律視為漏掛。
// 唯一例外是 POST /api/auth/login——它在 WebMvcConfig 被排除攔截，呼叫時還沒有身分可判定。
// 純讀取端點也要標，標的是全部四個角色（語意上等於「只要有 token」，因為無角色關聯的帳號
// 不得登入，見 User.md「登入契約」）。為何不乾脆留白：HandlerInterceptor 預設 allow-all，
// 漏掛不會有任何人提醒，留白就分不出「刻意開放」與「忘記掛」。標滿之後「沒有標註」才有意義。
// 端點與角色的對照依據是 User.md 的「權限矩陣」，該表是唯一真相來源。

// @Target：允許標註的位置。刻意只開放 METHOD——JwtInterceptor 用 getMethodAnnotation 讀，
// 只看得到方法層級的標註。若開放 TYPE，標在類別上會編譯通過但完全不生效，
// 變成「以為有保護、其實沒有」的安全性陷阱；限制在 METHOD 讓誤用直接編譯失敗。
// 日後要支援類別層級，須同時改攔截器（補讀 handlerMethod.getBeanType() 上的標註）。
@Target(ElementType.METHOD)
// @Retention RUNTIME：標註資訊要保留到執行期，攔截器才能在跑的時候用反射讀到它。
//（預設的 CLASS 只留到位元碼、執行期讀不到）
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {

    /**
     * 需要的角色，語意為 <b>OR</b>——具備其中任一個即放行。
     * 寫成 {@code @RequireRole({"LEADER", "ADMIN"})}；單一角色可省略大括號。
     *
     * <p>用陣列而非單一字串，是因為權限矩陣（User.md）每一列都是「某角色<b>或</b> ADMIN」，
     * 單值型別連寫都寫不出來。
     *
     * <p><b>判定語意為「在任一營業所具備該角色」，不比對本次操作的營業所。</b>
     * 攔截器拿不到本次要動的營業所——它可能在 query、在 body、或只存在於待操作的單據上
     * （例如領貨只帶 locationCode，須查 Location 主檔才知道所屬營業所）。
     * 因此本標註只回答「他是不是這個工種」，「他能不能動這筆資料」一律由 Service 層負責。
     *
     * <p>這代表 <b>Service 層的資料範圍檢查是必要的，不是加分項</b>：
     * 在任一營業所具備某角色的人，打得到所有該角色端點的 Controller 門口。
     * 完整理由與端點盤點見 {@code docs/requirements/specification/master/User.md}「資料範圍授權」。
     */
    String[] value();
}
