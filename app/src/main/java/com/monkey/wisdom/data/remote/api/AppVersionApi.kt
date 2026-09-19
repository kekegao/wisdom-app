package com.monkey.wisdom.data.remote.api

import com.monkey.wisdom.data.model.ApiResult
import com.monkey.wisdom.data.model.AppVersionInfo
import retrofit2.http.Body
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
     */
    @POST("appVersion/latest")
    suspend fun latest(@Body body: HashMap<String, Any?>): ApiResult<AppVersionInfo>
}
