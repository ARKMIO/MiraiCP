package org.example.mirai.plugin

import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.contact.NormalMember
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.events.*
import net.mamoe.mirai.message.code.MiraiCode
import net.mamoe.mirai.message.data.Image
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiInternalApi
import net.mamoe.mirai.utils.MiraiLogger
import net.mamoe.mirai.utils.MiraiLogger.Companion.setDefaultLoggerCreator
import net.mamoe.mirai.utils.OverFileSizeMaxException
import net.mamoe.mirai.utils.PlatformLogger
import java.io.File
import java.lang.Long.valueOf
import java.util.*
import kotlin.concurrent.schedule
import org.fusesource.jansi.AnsiConsole

object KotlinMain {
    private var friend_cache = ArrayList<NormalMember>(0)
    lateinit var dll_name:String
    private lateinit var AIbot: Bot
    private lateinit var cpp: CPP_lib
    lateinit var logger:MiraiLogger
    //日志部分实现
    fun BasicSendLog(log: String) {
        logger.info(log)
    }

    fun SendWarning(log: String) {
        logger.warning(log)
    }

    fun SendError(log: String) {
        logger.error(log)
    }

    //发送消息部分实现
    suspend fun Send(message: String, id: Long) {
        //反向调用
        logger.info("Send message for($id) is $message")
        val f = AIbot.getFriend(id) ?: let {
            logger.error("发送消息找不到好友，位置:K-Send()，id:$id")
            return
        }
        f.sendMessage(MiraiCode.deserializeMiraiCode(message))
    }

    suspend fun Send(message: String, id: Long, gid: Long) {
        //反向调用
        logger.info("Send message for a member($id) is $message")
        for (a in friend_cache) {
            if (a.id == id && a.group.id == gid) {
                a.sendMessage(message)
                return
            }
        }
        val G = AIbot.getGroup(gid) ?: let {
            logger.error("发送消息找不到群聊，位置K-Send()，id:$gid")
            return
        }
        val f = G[id] ?: let {
            logger.error("发送消息找不到群成员，位置K-Send()，id:$id，gid:$gid")
            return
        }
        f.sendMessage(MiraiCode.deserializeMiraiCode(message))
    }

    suspend fun SendG(message: String, id: Long) {
        logger.info("Send message for Group($id) is $message")
        val g = AIbot.getGroup(id) ?: let {
            logger.error("发送群消息异常找不到群组，位置K-SendG，gid:$id")
            return
        }
        g.sendMessage(MiraiCode.deserializeMiraiCode(message))
    }

    //取昵称或名片部分
    fun GetN(qqid: Long): String {
        val f = AIbot.getFriend(qqid) ?: let {
            logger.error("找不到对应好友的昵称，位置:K-GetN()，id:$qqid")
            return ""
        }
        return f.nick
    }

    fun GetNN(qqid: Long, groupid: Long): String {
        for (a in friend_cache) {
            if (a.id == qqid && a.group.id == groupid) {
                return a.nameCardOrNick
            }
        }

        val group = AIbot.getGroup(groupid) ?: let {
            logger.error("取群名片找不到对应群组，位置K-GetNN()，gid:$groupid")
            return ""
        }
        val member = group[qqid] ?: let {
            logger.error("取群名片找不到对应群成员，位置K-GetNN()，id:$qqid, gid:$groupid")
            return ""
        }
        return member.nameCard

    }

    //图片部分实现
    suspend fun uploadImgFriend(id: Long, file: String): String {
        val temp = AIbot.getFriend(id) ?: let {
            logger.error("发送图片找不到对应好友,位置:K-uploadImgFriend(),id:$id")
            return ""
        }
        return try {
            File(file).uploadAsImage(temp).imageId
        } catch (e: OverFileSizeMaxException) {
            logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
            ""
        } catch (e: NullPointerException) {
            logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
            ""
        }
    }

    suspend fun uploadImgGroup(id: Long, file: String): String {
        val temp = AIbot.getGroup(id) ?: let {
            logger.error("发送图片找不到对应群组,位置:K-uploadImgGroup(),id:$id")
            return ""
        }
        return try {
            File(file).uploadAsImage(temp).imageId
        } catch (e: OverFileSizeMaxException) {
            logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
            ""
        } catch (e: NullPointerException) {
            logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
            ""
        }
    }

    suspend fun uploadImgMember(id: Long, qqid: Long, file: String): String {
        val temp = AIbot.getGroup(id) ?: let {
            logger.error("发送图片找不到对应群组,位置:K-uploadImgGroup(),id:$id")
            return ""
        }
        val temp1 = temp[qqid] ?: let {
            logger.error("发送图片找不到目标成员,位置:K-uploadImgMember(),成员id:$qqid,群聊id:$id")
            return ""
        }
        return try {
            File(file).uploadAsImage(temp1).imageId
        } catch (e: OverFileSizeMaxException) {
            logger.error("图片文件过大超过30MB,位置:K-uploadImgGroup(),文件名:$file")
            ""
        } catch (e: NullPointerException) {
            logger.error("上传图片文件名异常,位置:K-uploadImgGroup(),文件名:$file")
            ""
        }
    }

    suspend fun QueryImg(id: String): String {
        return Image(id).queryUrl()
    }

    //定时任务
    fun scheduling(time: Long, id: Int) {
        Timer("SettingUp", false).schedule(time) {
            cpp.ScheduleTask(id)
        }
    }

    suspend fun mute(qqid: Long, groupid: Long, time: Int): String {
        val group = AIbot.getGroup(groupid) ?: let {
            logger.error("禁言找不到对应群组，位置K-mute()，gid:$groupid")
            return "E1"
        }
        val member = group[qqid] ?: let {
            logger.error("禁言找不到对应群成员，位置K-mute()，id:$qqid, gid:$groupid")
            return "E2"
        }
        try {
            member.mute(time)
        } catch (e: PermissionDeniedException) {
            logger.error("执行禁言失败机器人无权限，位置:K-mute()，目标群id:$groupid，目标成员id:$qqid")
            return "E3"
        } catch (e: IllegalStateException) {
            logger.error("执行禁言失败禁言时间超出0s~30d，位置:K-mute()，时间:$time")
            return "E4"
        }
        return "Y"
    }

    @MiraiInternalApi
    suspend fun main(id:Long, pass:String, path:String){
        setDefaultLoggerCreator { identity ->
            PlatformLogger(identity, AnsiConsole.out::println, true)
        }
        dll_name = path
        println("启动成功!")
        println("github存储库:https://github.com/Nambers/MiraiCP")
        if (!File(dll_name).exists()) {
            println("文件$dll_name 不存在")
            return
        }
        val bot = BotFactory.newBot(id, pass) {
            fileBasedDeviceInfo()
        }.alsoLogin()
        AIbot = bot
        logger=bot.logger
        cpp = CPP_lib()
        val gson = Gson()
        val globalEventChannel = bot.eventChannel
        logger.info(cpp.ver)//输出2333 正常
        //配置文件目录 "${dataFolder.absolutePath}/"
        globalEventChannel.subscribeAlways<GroupMessageEvent> {
            //群消息
            cpp.Event(
                gson.toJson(
                    Config.GroupMessage(
                        this.group.id,
                        this.sender.id,
                        this.message.serializeToMiraiCode()
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<MemberLeaveEvent.Kick> {
            friend_cache.add(this.member)
            cpp.Event(
                gson.toJson(
                    Config.MemberLeave(
                        this.group.id,
                        this.member.id,
                        1,
                        if (this.operator?.id == null) this.bot.id else this.operator!!.id
                    )
                )
            )
            friend_cache.remove(this.member)
        }
        globalEventChannel.subscribeAlways<MemberLeaveEvent.Quit> {
            friend_cache.add(this.member)
            cpp.Event(
                gson.toJson(
                    Config.MemberLeave(
                        this.group.id,
                        this.member.id,
                        2,
                        this.member.id
                    )
                )
            )
            friend_cache.remove(this.member)
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Retrieve> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        this.group.id,
                        this.member.id,
                        3,
                        this.member.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Active> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        this.group.id,
                        this.member.id,
                        2,
                        this.member.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<MemberJoinEvent.Invite> {
            cpp.Event(
                gson.toJson(
                    Config.MemberJoin(
                        this.group.id,
                        this.member.id,
                        1,
                        this.invitor.id
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<FriendMessageEvent> {
            //好友信息
            cpp.Event(
                gson.toJson(
                    Config.PrivateMessage(
                        this.sender.id,
                        this.message.serializeToMiraiCode()
                    )
                )
            )
        }
        globalEventChannel.subscribeAlways<NewFriendRequestEvent> {
            //自动同意好友申请
            val r = cpp.Event(
                gson.toJson(
                    Config.NewFriendRequest(
                        this.fromId,
                        this.message
                    )
                )
            )
            when (r) {
                "true" -> accept()
                "false" -> reject()
                else -> {
                    logger.error("NewFriendRequestEvent return unknown")
                    reject()
                }
            }
        }
        globalEventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            //自动同意加群申请
            val r = cpp.Event(
                gson.toJson(
                    Config.GroupInvite(
                        this.groupId,
                        this.invitorId
                    )
                )
            )
            when (r) {
                "true" -> accept()
                "false" -> ignore()
                else -> {
                    logger.error("BotInvitedJoinGroupRequestEvent return unknown")
                    ignore()
                }
            }
        }

    }
}

@MiraiInternalApi
fun main(args: Array<String>){
    // qqid, passworld, dllpath
    if(args.size != 3){
        println("参数不足或多余，请提供[botqqid, password, dllpath]")
        return
    }
    println("正在启动\n机器人qqid:${args[0]}\n机器人qq密码${args[1]}\nc++部分dll存放地址${args[2]}")
    runBlocking {
        try {
            KotlinMain.main(valueOf(args[0]), args[1], args[2])
        }catch (e:NumberFormatException){
            println("${args[0]}不是一个有效的qq号数字")
            return@runBlocking
        }
    }
}