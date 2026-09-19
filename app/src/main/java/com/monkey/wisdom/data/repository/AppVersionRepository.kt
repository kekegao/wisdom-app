package com.monkey.wisdom.data.repository

import com.monkey.wisdom.core.common.AppResult
import com.monkey.wisdom.data.model.AppVersionInfo

/** APP 版本仓库 */
interface AppVersionRepository {

    /** 查询服务端最新版本信息 */
    suspend fun latest(): AppResult<AppVersionInfo>
}
