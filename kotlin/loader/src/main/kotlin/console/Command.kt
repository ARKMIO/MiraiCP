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

package tech.eritquearcus.miraicp.loader.console

import com.google.gson.Gson
import net.mamoe.mirai.BotFactory
import tech.eritquearcus.miraicp.loader.KotlinMain
import tech.eritquearcus.miraicp.loader.login
import tech.eritquearcus.miraicp.shared.PublicShared
import java.time.Duration
import java.time.LocalDateTime
import kotlin.system.exitProcess

object Command {
    private const val ch = " "
    private fun printHelp() {
        val message = listOf(
            "exit" to "退出",
            "status" to "查看loader状态",
            "login <qqid>" to "登录已经配置在配置文件的qq",
            "accountList" to "查看配置文件里的qq"
        )
        val prefixPlaceholder = String(CharArray(
            message.maxOfOrNull { it.first.length }!! + 3
        ) { ' ' })

        // From Mamoe Technologies and contributors
        fun printOption(optionName: String, value: String) {
            if (optionName == "") {
                println(value)
                return
            }
            print(optionName)
            print(prefixPlaceholder.substring(optionName.length))
            val lines = value.split('\n').iterator()
            if (lines.hasNext()) println(lines.next())
            lines.forEach { line ->
                print(prefixPlaceholder)
                println(line)
            }
        }
        message.forEach { (optionName, value) ->
            printOption(optionName, value)
        }
    }

    fun parse(order: String) {
        val re = order.split(ch)
        when (re.size) {
            0 -> unknown(order)
            1 -> pureOrder(re[0])
            2 -> oneParamOrder(arrayOf(re[0], re[1]))
            else -> unknown(order)
        }
    }

    private fun unknown(order: String) {
        PublicShared.logger.error("未知命令: '$order', 输入 \"help\" 获取命令帮助")
    }

    private fun error(order: String, reason: String){
        PublicShared.logger.error("命令错误: '$order', $reason")
    }

    private fun pureOrder(order: String) {
        when (order) {
            "exit" -> {
                PublicShared.onDisable()
                exitProcess(0)
            }
            "help" -> printHelp()
            "status" -> {
                val s = Duration.between(Console.start, LocalDateTime.now()).seconds
                println("该Loader已经持续运行 " + String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60)) + " 啦")
            }
            "accountList" -> KotlinMain.loginAccount.let { acs->
                val gson = Gson()
                acs.forEach { println(gson.toJson(it)) }
            }
            else -> unknown(order)
        }
    }

    private fun login(id:Long){
        KotlinMain.loginAccount.first { it.id == id && (it.logined == null || it.logined == false)}.login()
    }

    private fun oneParamOrder(order: Array<String>) {
        when(order[0]){
            "login" -> {
                val id = try {
                    order[1].toLong()
                }catch(e:NumberFormatException){
                    error(order.joinToString(" "),order[1] + "不是有效的qq号")
                    return
                }
                try{
                    login(id)
                }catch(e:NoSuchElementException ){
                    error(order.joinToString(" "), "config文件中没找到关于$id 的定义或该id已经通过自动登录登录")
                }
                KotlinMain.logined = true
            }
            else -> unknown(order.joinToString(" "))
        }
    }
}