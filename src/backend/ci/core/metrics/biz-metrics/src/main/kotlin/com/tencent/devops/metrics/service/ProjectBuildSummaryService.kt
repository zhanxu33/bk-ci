/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.metrics.service

import com.tencent.devops.common.event.pojo.measure.ProjectUserOperateMetricsData
import com.tencent.devops.metrics.pojo.vo.BaseQueryReqVO
import com.tencent.devops.metrics.pojo.vo.ProjectUserCountV0
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

interface ProjectBuildSummaryService {

    /**
     * 保存项目构建数
     */
    fun saveProjectBuildCount(
        projectId: String,
        trigger: String?
    )

    /**
     * 保存项目用户
     */
    fun saveProjectUser(
        projectId: String,
        userId: String,
        operate: String,
        theDate: LocalDate
    )

    /**
     * 保存用户操作度量数据
     */
    fun saveProjectUserOperateMetrics(
        projectUserOperateMetricsMap: Map<String, Map<ProjectUserOperateMetricsData, AtomicInteger>>
    )

    /**
     * 获取项目活跃用户数
     */
    fun getProjectActiveUserCount(
        baseQueryReq: BaseQueryReqVO
    ): ProjectUserCountV0?
}
