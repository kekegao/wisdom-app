package com.monkey.wisdom.data.remote.api

import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.AppVersionInfo
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * APP 版本相关接口。
 *
 * baseUrl 与业务接口一致（BuildConfig.API_BASE_URL）。
 */
interface AppVersionApi {

    /**
     * 查询最新版本。
     *
     * POST /appVersion/latest
     *
     * 标记 X-Skip-Session-Check：版本查询属于公开接口，即使返回 401 也不判定为登录态失效，
     * 避免因后端未配置鉴权白名单而把用户踢回登录页。
     */
    @Headers(
        "X-Skip-Session-Check: true",
        "Accept: application/json;charset=UTF-8",
    )
    @POST("appVersion/latest")
    suspend fun latest(@Body body: HashMap<String, Any?>): ApiResult<AppVersionInfo>
}
