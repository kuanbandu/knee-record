package com.kuanbandu.kneerecord.engine

/**
 * 跟练页之间传递本次会话用的临时持有者。
 * 只活在内存里，不落盘 —— 中途退出 App 本次训练就作废，不会污染记录。
 */
object SessionHolder {
    var session: Session? = null
}
