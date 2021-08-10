package com.michaeltroger.gruenerpass.pager.certificates;

import com.xwray.groupie.GroupieAdapter

class CertificateAdapter : GroupieAdapter() {
    var list = listOf<String>()

    fun setData(idList: List<String>) {
        this.list = idList
    }
}
