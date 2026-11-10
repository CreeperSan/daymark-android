package com.creepersan.memorialday.module.anniversary

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject

/** 将当前进程中已保存的纪念日同步给首页，不触发额外列表请求。 */
object AnniversaryUpdates {
    private val deletions = PublishSubject.create<Long>().toSerialized()
    /** 订阅已删除的记录。 */
    fun observeDeletions(): Observable<Long> = deletions.hide()
    fun publishDeletion(id: Long) { deletions.onNext(id) }

    private val changes = PublishSubject.create<Anniversary>().toSerialized()

    /** 订阅保存结果；页面销毁时由订阅方释放。 */
    fun observe(): Observable<Anniversary> = changes.hide()

    /** 保存成功后发布服务端返回的最新数据。 */
    fun publish(item: Anniversary) { changes.onNext(item) }
}
