package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.core.common.safeDataCall
import com.monkey.wisdom.data.model.AppVersionInfo
import com.monkey.wisdom.data.remote.api.AppVersionApi

/** APP 版本仓库实现 */
class AppVersionRepositoryImpl(
    private val appVersionApi: AppVersionApi,
) : AppVersionRepository {

    override suspend fun latest(): AppResult<AppVersionInfo> =
        safeDataCall(fallbackMessage = "检查更新失败，请稍后重试") { appVersionApi.latest(hashMapOf()) }
}
