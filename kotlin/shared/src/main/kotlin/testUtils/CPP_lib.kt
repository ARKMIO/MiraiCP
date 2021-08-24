/*
 * Copyright (C) 2020-2021 Eritque arcus and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or any later version(in your opinion).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package tech.eritquearcus.miraicp.shared.testUtils

import com.google.gson.Gson
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.utils.MiraiLogger
import org.json.JSONObject
import tech.eritquearcus.miraicp.shared.Config
import tech.eritquearcus.miraicp.shared.PluginConfig
import tech.eritquearcus.miraicp.shared.PublicShared

class CPP_lib(
    val dll_path: String,
    val dependencies: List<String>?
) {
    var config: PluginConfig

    init {
        dependencies?.forEach {
            System.load(it)
        }
        System.load(dll_path)
        config = Gson().fromJson(Verify(), PluginConfig::class.java)
    }

    fun showInfo(logger: MiraiLogger = PublicShared.logger, version: String = PublicShared.now_tag) {
        logger.info("⭐已加载插件: ${config.name}")
        logger.info("⭐作者: ${config.author}")
        logger.info("⭐版本: ${config.version}")
        logger.info("⭐本机地址: $dll_path")
        logger.info("⭐依赖dll: ${dependencies?.joinToString(" ") ?: let { "null" }}")
        if (config.description != "")
            logger.info("⭐描述: ${config.description}")
        if (config.time != "")
            logger.info("⭐发行时间: ${config.time}")
        if (config.MiraiCPversion != version) {
            logger.warning("Warning: 当前MiraiCP框架版本($version)和加载的插件的C++ SDK(${config.MiraiCPversion})不一致")
        }
    }

    companion object {
        private val groupMessageSouce = """
        {"kind":"GROUP","botId":692928873,"ids":[3926],"internalIds":[1921344034],"time":1629788808,"fromId":692928873,"targetId":788189105,"originalMessage":[{"type":"PlainText","content":"x"}]}
    """.trimIndent()

        fun basicSendLog(log: String, botid: Long, name: String = "") {
            when (botid) {
                -1L -> println("I: $name : $log")
                -2L -> println("I: $log")
                else -> println("I: $botid : $log")
            }
        }

        fun sendWarning(log: String, botid: Long, name: String = "") {
            when (botid) {
                -1L -> println("W: $name : $log")
                -2L -> println("W: $log")
                else -> println("W: $botid : $log")
            }
        }

        fun sendError(log: String, botid: Long, name: String = "") {
            when (botid) {
                -1L -> println("E: $name : $log")
                -2L -> println("E: $log")
                else -> println("E: $botid : $log")
            }
        }

        //send MiraiCode
        private fun KSend(source: String, miraiCode: Boolean): String = runBlocking {
            println()
            delay(190)
            groupMessageSouce
        }

        //recall messageSource
        private fun KRecall(source: String): String = "Y"

        //查询图片下载链接
        private fun KQueryImgUrl(id: String): String = "https:\\"

        @JvmStatic
        fun KSendLog(log: String, level: Int) {
            val j = JSONObject(log)
            if (j.getLong("id") == -1L)
                when (level) {
                    0 -> basicSendLog(j.getString("log"), j.getLong("id"), j.getString("name"))
                    1 -> sendWarning(j.getString("log"), j.getLong("id"), j.getString("name"))
                    2 -> sendError(j.getString("log"), j.getLong("id"), j.getString("name"))
                }
            else
                when (level) {
                    0 -> basicSendLog(j.getString("log"), j.getLong("id"))
                    1 -> sendWarning(j.getString("log"), j.getLong("id"))
                    2 -> sendError(j.getString("log"), j.getLong("id"))
                }
        }

        private fun KRefreshInfo(source: String, quit: Boolean): String =
            runBlocking {
                PublicShared.RefreshInfo(PublicShared.gson.fromJson(source, Config.Contact::class.java), quit)
            }

        private fun KUploadImg(fileName: String, source: String): String = "imageid"

        private fun KSendFile(source: String, contactSource: String): String =
            runBlocking {
                val t = JSONObject(source)
                PublicShared.sendFile(
                    t.getString("path"),
                    t.getString("filename"),
                    PublicShared.gson.fromJson(contactSource, Config.Contact::class.java)
                )
            }

        private fun KRemoteFileInfo(source: String, contactSource: String): String =
            runBlocking {
                val t = JSONObject(source)
                return@runBlocking PublicShared.remoteFileInfo(
                    t.getString("path"),
                    t.getString("id"),
                    PublicShared.gson.fromJson(contactSource, Config.Contact::class.java)
                )
            }

        //mute member
        private fun KMuteM(time: Int, contactSource: String): String =
            runBlocking {
                PublicShared.mute(time, PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))
            }

        //query the permission of a member in a group
        private fun KQueryM(contactSource: String): String =
            PublicShared.kqueryM(PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))

        //kick a member
        private fun KKickM(message: String, contactSource: String): String =
            runBlocking {
                PublicShared.kkick(message, PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))
            }

        //query the member list of a group
        private fun KQueryML(contactSource: String): String {
            return PublicShared.QueryML(PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))
        }

        // query the friend lst of the bot
        private fun KQueryBFL(botid: Long): String {
            return PublicShared.QueryBFL(botid)
        }

        // query the group list of the bot
        private fun KQueryBGL(botid: Long): String {
            return PublicShared.QueryBGL(botid)
        }

        //query the owner of a group
        private fun KQueryOwner(contactSource: String): String =
            PublicShared.getowner(PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))

        //build forward message
        private fun KBuildforward(text: String, botid: Long): String =
            runBlocking {
                PublicShared.buildforwardMsg(text, botid)
            }

        // new friend request operation
        private fun KNfroperation(text: String, sign: Boolean): String =
            runBlocking {
                val tmp = PublicShared.gson.fromJson(text, Config.NewFriendRequestSource::class.java)
                if (sign) PublicShared.accpetFriendRequest(tmp)
                else PublicShared.rejectFriendRequest(tmp)
            }

        // Group invite operation
        private fun KGioperation(text: String, sign: Boolean): String =
            runBlocking {
                if (sign) PublicShared.accpetGroupInvite(
                    PublicShared.gson.fromJson(
                        text,
                        Config.GroupInviteSource::class.java
                    )
                )
                else PublicShared.rejectGroupInvite(
                    PublicShared.gson.fromJson(
                        text,
                        Config.GroupInviteSource::class.java
                    )
                )
            }

        private fun KSendWithQuote(messageSource: String, msg: String, sign: String): String =
            runBlocking {
                PublicShared.sendWithQuote(messageSource, msg, sign)
            }


        private fun KUpdateSetting(contactSource: String, source: String): String =
            runBlocking {
                PublicShared.groupSetting(
                    PublicShared.gson.fromJson(
                        contactSource,
                        Config.Contact::class.java
                    ), source
                )
            }

        private fun KUploadVoice(contactSource: String, source: String): String =
            JSONObject(source).let { tmp ->
                return runBlocking {
                    PublicShared.uploadVoice(
                        tmp.getString("path"),
                        PublicShared.gson.fromJson(contactSource, Config.Contact::class.java)
                    )
                }
            }

        private fun KAnnouncement(identify: String, source: String?): String =
            PublicShared.gson.fromJson(identify, Config.IdentifyA::class.java).let { i ->
                return when (i.type) {
                    1 -> {
                        runBlocking {
                            PublicShared.deleteOnlineAnnouncement(i)
                        }
                    }
                    2 -> {
                        runBlocking {
                            PublicShared.publishOfflineAnnouncement(
                                i,
                                PublicShared.gson.fromJson(source!!, Config.BriefOfflineA::class.java)
                            )
                        }
                    }
                    else -> {
                        "EA"
                    }
                }
            }

        private fun KNudge(contactSource: String): String =
            PublicShared.sendNudge(PublicShared.gson.fromJson(contactSource, Config.Contact::class.java))

        private fun KNextMsg(contactSource: String, time: Long, halt: Boolean): String =
            PublicShared.nextMsg(PublicShared.gson.fromJson(contactSource, Config.Contact::class.java), time, halt)

        enum class operation_code {
            /// 撤回信息
            Recall,

            /// 发送信息
            Send,

            /// 查询信息接口
            RefreshInfo,

            /// 上传图片
            UploadImg,

            /// 取好友列表
            QueryBFL,

            /// 取群组列表
            QueryBGL,

            /// 上传文件
            SendFile,

            /// 查询文件信息
            RemoteFileInfo,

            /// 查询图片下载地址
            QueryImgUrl,

            /// 禁言
            MuteM,

            /// 查询权限
            QueryM,

            /// 踢出
            KickM,

            /// 取群主
            QueryOwner,

            /// 上传语音
            UploadVoice,

            /// 查询群成员列表
            QueryML,

            /// 群设置
            GroupSetting,

            /// 构建转发信息
            Buildforward,

            /// 好友申请事件
            Nfroperation,

            /// 群聊邀请事件
            Gioperation,

            /// 回复(引用并发送)
            SendWithQuote,

            /// 群公告操作
            Announcement,

            /// 定时任务
            Timer,

            ///发送戳一戳
            Nudge,

            /// 好友对象下一条消息
            NextMsg
        }

        @JvmStatic
        fun KOperation(content: String): String {
            try {
                val j = JSONObject(content)
                val root = j.getJSONObject("data")
                return when (j.getInt("type")) {
                    /// 撤回信息
                    operation_code.Recall.ordinal -> KRecall(root.getString("source"))
                    /// 发送信息
                    operation_code.Send.ordinal -> KSend(root.getString("source"), root.getBoolean("miraiCode"))
                    /// 查询信息接口
                    operation_code.RefreshInfo.ordinal -> KRefreshInfo(root.getString("source"), root.has("quit"))
                    /// 上传图片
                    operation_code.UploadImg.ordinal -> KUploadImg(root.getString("fileName"), root.getString("source"))
                    /// 取好友列表
                    operation_code.QueryBFL.ordinal -> KQueryBFL(root.getLong("botid"))
                    /// 取群组列表
                    operation_code.QueryBGL.ordinal -> KQueryBGL(root.getLong("botid"))
                    /// 上传文件
                    operation_code.SendFile.ordinal -> KSendFile(
                        root.getString("source"),
                        root.getString("contactSource")
                    )
                    /// 查询文件信息
                    operation_code.RemoteFileInfo.ordinal -> KRemoteFileInfo(
                        root.getString("source"),
                        root.getString("contactSource")
                    )
                    /// 查询图片下载地址
                    operation_code.QueryImgUrl.ordinal -> KQueryImgUrl(root.getString("id"))
                    /// 禁言
                    operation_code.MuteM.ordinal -> KMuteM(root.getInt("time"), root.getString("contactSource"))
                    /// 查询权限
                    operation_code.QueryM.ordinal -> KQueryM(root.getString("contactSource"))
                    /// 踢出
                    operation_code.KickM.ordinal -> KKickM(root.getString("message"), root.getString("contactSOurce"))
                    /// 取群主
                    operation_code.QueryOwner.ordinal -> KQueryOwner(root.getString("contactSource"))
                    /// 上传语音
                    operation_code.UploadVoice.ordinal -> KUploadVoice(
                        root.getString("contactSource"),
                        root.getString("source")
                    )
                    /// 查询群成员列表
                    operation_code.QueryML.ordinal -> KQueryML(root.getString("contactSource"))
                    /// 群设置
                    operation_code.GroupSetting.ordinal -> KUpdateSetting(
                        root.getString("contactSource"),
                        root.getString("source")
                    )
                    /// 构建转发信息
                    operation_code.Buildforward.ordinal -> KBuildforward(root.getString("text"), root.getLong("botid"))
                    /// 好友申请事件
                    operation_code.Nfroperation.ordinal -> KNfroperation(
                        root.getString("text"),
                        root.getBoolean("sign")
                    )
                    /// 群聊邀请事件
                    operation_code.Gioperation.ordinal -> KGioperation(root.getString("text"), root.getBoolean("sign"))
                    /// 回复(引用并发送)
                    operation_code.SendWithQuote.ordinal -> KSendWithQuote(
                        root.getString("messageSource"),
                        root.getString("msg"),
                        root.getString("sign")
                    )
                    /// 群公告操作
                    operation_code.Announcement.ordinal -> KAnnouncement(
                        root.getString("identify"),
                        if (root.has("source")) root.getString("source") else null
                    )
                    /// 定时任务
                    operation_code.Timer.ordinal -> PublicShared.scheduling(root.getLong("time"), root.getString("msg"))
                    /// 发送戳一戳
                    operation_code.Nudge.ordinal -> KNudge(root.getString("contactSource"))
                    /// 好友下一条信息
                    operation_code.NextMsg.ordinal -> KNextMsg(
                        root.getString("contactsource"),
                        root.getLong("time"),
                        root.getBoolean("halt")
                    )
                    else -> "EA"
                }
            } catch (e: Exception) {
                println(e.message)
                println(content)
                e.printStackTrace()
                return "EA"
            }
        }
    }

    private external fun Verify(): String
    external fun Event(content: String): String
    external fun PluginDisable(): Void
}