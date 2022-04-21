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
 */

package com.tencent.devops.stream.trigger.git.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.api.scm.ServiceGitResource
import com.tencent.devops.repository.api.scm.ServiceScmOauthResource
import com.tencent.devops.repository.pojo.enums.RepoAuthType
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.scm.enums.GitAccessLevelEnum
import com.tencent.devops.scm.utils.code.git.GitUtils
import com.tencent.devops.stream.common.exception.ErrorCodeEnum
import com.tencent.devops.stream.trigger.git.pojo.ApiRequestRetryInfo
import com.tencent.devops.stream.trigger.git.pojo.StreamGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitChangeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCommitInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitCred
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrChangeInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitMrInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitProjectInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitProjectUserInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitRevisionInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitTreeFileInfo
import com.tencent.devops.stream.trigger.git.pojo.tgit.TGitUserInfo
import com.tencent.devops.stream.util.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TGitApiService @Autowired constructor(
    private val client: Client
) : StreamGitApiService {

    companion object {
        private val logger = LoggerFactory.getLogger(TGitApiService::class.java)
    }

    /**
     * 通过凭据获取可以直接使用的token
     */
    override fun getToken(
        cred: StreamGitCred
    ): String {
        return cred.toToken()
    }

    override fun getGitProjectInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        retry: ApiRequestRetryInfo
    ): TGitProjectInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get project $gitProjectId fail",
            apiErrorCode = ErrorCodeEnum.GET_PROJECT_INFO_ERROR
        ) {
            client.get(ServiceGitResource::class).getProjectInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                gitProjectId = gitProjectId
            ).data
        }?.let {
            TGitProjectInfo(it)
        }
    }

    override fun getGitCommitInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        sha: String,
        retry: ApiRequestRetryInfo
    ): TGitCommitInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get commit info $sha fail",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_INFO_ERROR
        ) {
            client.get(ServiceGitResource::class).getRepoRecentCommitInfo(
                repoName = gitProjectId,
                sha = sha,
                token = cred.toToken(),
                tokenType = cred.toTokenType()
            ).data
        }?.let { TGitCommitInfo(it) }
    }

    override fun getUserInfoByToken(cred: StreamGitCred): TGitUserInfo? {
        return client.get(ServiceGitResource::class).getUserInfoByToken(
            cred.toToken(),
            cred.toTokenType()
        ).data?.let { TGitUserInfo(id = it.id.toString(), username = it.username!!) }
    }

    override fun getProjectUserInfo(
        cred: StreamGitCred,
        userId: String,
        gitProjectId: String
    ): TGitProjectUserInfo {
        return client.get(ServiceGitResource::class).getProjectUserInfo(
            token = cred.toToken(),
            tokenType = cred.toTokenType(),
            gitProjectId = gitProjectId,
            userId = userId
        ).data!!.let {
            TGitProjectUserInfo(it.accessLevel)
        }
    }

    override fun getMrInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get mr $mrId info error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_INFO
        ) {
            client.get(ServiceGitResource::class).getMergeRequestInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }?.let {
            TGitMrInfo(
                mergeStatus = it.mergeStatus,
                baseCommit = it.baseCommit
            )
        }
    }

    override fun getMrChangeInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        mrId: String,
        retry: ApiRequestRetryInfo
    ): TGitMrChangeInfo? {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get mr $mrId changeInfo error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_MERGE_CHANGE_INFO
        ) {
            client.get(ServiceGitResource::class).getMergeRequestChangeInfo(
                token = cred.toToken(),
                tokenType = cred.toTokenType(),
                repoName = gitProjectId,
                mrId = mrId.toLong()
            ).data
        }?.let {
            TGitMrChangeInfo(
                files = it.files.map { f ->
                    TGitChangeFileInfo(f)
                }
            )
        }
    }

    override fun getFileTree(
        cred: StreamGitCred,
        gitProjectId: String,
        path: String?,
        ref: String?,
        recursive: Boolean,
        retry: ApiRequestRetryInfo
    ): List<TGitTreeFileInfo> {
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get $path file tree error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_TREE_ERROR
        ) {
            client.get(ServiceGitResource::class).getGitFileTree(
                gitProjectId = gitProjectId.toLong(),
                path = path ?: "",
                token = cred.toToken(),
                ref = ref,
                recursive = recursive,
                tokenType = cred.toTokenType()
            ).data ?: emptyList()
        }.map { TGitTreeFileInfo(it) }
    }

    override fun getFileContent(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String,
        retry: ApiRequestRetryInfo
    ): String {
        cred as TGitCred
        return doRetryFun(
            retry = retry,
            log = "$gitProjectId get yaml $fileName fail",
            apiErrorCode = ErrorCodeEnum.GET_YAML_CONTENT_ERROR
        ) {
            client.get(ServiceGitResource::class).getGitFileContent(
                token = cred.toToken(),
                authType = if (cred.useAccessToken) {
                    RepoAuthType.OAUTH
                } else {
                    RepoAuthType.SSH
                },
                repoName = gitProjectId,
                ref = ref,
                filePath = fileName
            ).data!!
        }
    }

    override fun getFileInfo(
        cred: StreamGitCred,
        gitProjectId: String,
        fileName: String,
        ref: String?,
        retry: ApiRequestRetryInfo
    ): TGitFileInfo? {
        return doRetryFun(
            retry = retry,
            log = "getFileInfo: [$gitProjectId|$fileName][$ref] error",
            apiErrorCode = ErrorCodeEnum.GET_GIT_FILE_INFO_ERROR
        ) {
            client.get(ServiceGitResource::class).getGitFileInfo(
                gitProjectId = gitProjectId,
                filePath = fileName,
                token = cred.toToken(),
                ref = ref,
                tokenType = cred.toTokenType()
            ).data
        }?.let { TGitFileInfo(content = it.content, blobId = it.blobId) }
    }

    override fun getProjectList(
        cred: StreamGitCred,
        search: String?,
        minAccessLevel: GitAccessLevelEnum?
    ): List<TGitProjectInfo>? {
        return client.get(ServiceGitResource::class).getGitCodeProjectList(
            accessToken = cred.toToken(),
            page = null,
            pageSize = null,
            search = search,
            orderBy = null,
            sort = null,
            owned = null,
            minAccessLevel = minAccessLevel
        ).data?.map {
            TGitProjectInfo(
                gitProjectId = it.id.toString(),
                defaultBranch = it.defaultBranch,
                gitHttpUrl = it.httpUrlToRepo ?: "",
                name = it.name ?: "",
                gitSshUrl = it.sshUrlToRepo,
                homepage = it.webUrl,
                gitHttpsUrl = it.httpsUrlToRepo,
                description = it.description,
                avatarUrl = it.avatarUrl,
                pathWithNamespace = it.pathWithNamespace,
                nameWithNamespace = it.nameWithNamespace ?: ""
            )
        }
    }

    override fun getLatestRevision(
        pipelineId: String,
        projectName: String,
        gitUrl: String,
        branch: String,
        userName: String,
        enableUserId: String,
        retry: ApiRequestRetryInfo
    ): TGitRevisionInfo? {
        return doRetryFun(
            retry = retry,
            log = "timer|[$pipelineId] get latestRevision fail",
            apiErrorCode = ErrorCodeEnum.GET_GIT_LATEST_REVISION_ERROR
        ) {
            client.get(ServiceScmOauthResource::class)
                .getLatestRevision(
                    token = TGitCred(userId = enableUserId).toToken(),
                    projectName = GitUtils.getProjectName(gitUrl),
                    url = gitUrl,
                    type = ScmType.CODE_GIT,
                    branchName = branch,
                    userName = userName,
                    region = null,
                    privateKey = null,
                    passPhrase = null,
                    additionalPath = null
                ).data?.let { TGitRevisionInfo(it) }
        }
    }

    /**
     * 获取两个commit之间的差异文件
     * @param from 旧commit
     * @param to 新commit
     * @param straight true：两个点比较差异，false：三个点比较差异。默认是 false
     */
    fun getCommitChangeList(
        cred: TGitCred,
        gitProjectId: String,
        from: String,
        to: String,
        straight: Boolean,
        page: Int,
        pageSize: Int,
        retry: ApiRequestRetryInfo
    ): List<TGitChangeFileInfo> {
        return doRetryFun(
            retry = retry,
            log = "getCommitChangeFileListRetry from: $from to: $to error",
            apiErrorCode = ErrorCodeEnum.GET_COMMIT_CHANGE_FILE_LIST_ERROR
        ) {
            client.get(ServiceGitResource::class).getChangeFileList(
                cred.toToken(),
                cred.toTokenType(),
                gitProjectId = gitProjectId,
                from = from,
                to = to,
                straight = straight,
                page = page,
                pageSize = pageSize
            ).data ?: emptyList()
        }.map { TGitChangeFileInfo(it) }
    }

    /**
     * 为mr添加评论
     */
    fun addMrComment(
        cred: TGitCred,
        gitProjectId: String,
        mrId: Long,
        mrBody: String
    ) {
        return client.get(ServiceGitResource::class).addMrComment(
            token = cred.toToken(),
            gitProjectId = gitProjectId,
            mrId = mrId,
            mrBody = mrBody,
            tokenType = cred.toTokenType()
        )
    }

    private fun StreamGitCred.toToken(): String {
        this as TGitCred
        if (this.accessToken != null) {
            return this.accessToken
        }
        return client.get(ServiceOauthResource::class).gitGet(this.userId!!).data!!.accessToken
    }

    private fun StreamGitCred.toTokenType(): TokenTypeEnum {
        this as TGitCred
        return if (this.useAccessToken) {
            TokenTypeEnum.OAUTH
        } else {
            TokenTypeEnum.PRIVATE_KEY
        }
    }

    private fun <T> doRetryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        return if (retry.retry) {
            retryFun(
                retry = retry,
                log = log,
                apiErrorCode = apiErrorCode
            ) {
                action()
            }
        } else {
            action()
        }
    }

    private fun <T> retryFun(
        retry: ApiRequestRetryInfo,
        log: String,
        apiErrorCode: ErrorCodeEnum,
        action: () -> T
    ): T {
        try {
            return RetryUtils.clientRetry(
                retry.retryTimes,
                retry.retryPeriodMills
            ) {
                action()
            }
        } catch (e: ClientException) {
            logger.warn("retry 5 times $log: ${e.message} ")
            throw ErrorCodeException(
                errorCode = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.errorCode.toString(),
                defaultMessage = ErrorCodeEnum.DEVNET_TIMEOUT_ERROR.formatErrorMessage
            )
        } catch (e: RemoteServiceException) {
            logger.warn("GIT_API_ERROR $log: ${e.message} ")
            throw ErrorCodeException(
                statusCode = e.httpStatus,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.errorMessage}"
            )
        } catch (e: CustomException) {
            logger.warn("GIT_SCM_ERROR $log: ${e.message} ")
            throw ErrorCodeException(
                statusCode = e.status.statusCode,
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = "$log: ${e.message}"
            )
        } catch (e: Throwable) {
            logger.error("retryFun error $log: ${e.message} ")
            throw ErrorCodeException(
                errorCode = apiErrorCode.errorCode.toString(),
                defaultMessage = if (e.message.isNullOrBlank()) {
                    "$log: ${apiErrorCode.formatErrorMessage}"
                } else {
                    "$log: ${e.message}"
                }
            )
        }
    }
}
