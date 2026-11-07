package com.creepersan.memorialday.extension

import io.reactivex.rxjava3.disposables.Disposable


fun Disposable?.disposeIfNeed() {
    if (this?.isDisposed == false) {
        this.dispose()
    }
}

