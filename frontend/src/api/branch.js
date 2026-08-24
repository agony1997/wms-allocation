import {httpDelete, httpGet, httpPost, httpPut} from '@/api/httpClient'

export function findAllBranches() {
    return httpGet('/branches')
}

export function findBranchByCode(branchCode) {
    return httpGet(`/branches/${branchCode}`)
}

export function createBranch(data) {
    return httpPost('/branches', data)
}

export function updateBranch(branchCode, data) {
    return httpPut(`/branches/${branchCode}`, data)
}

export function deleteBranch(branchCode) {
    return httpDelete(`/branches/${branchCode}`)
}