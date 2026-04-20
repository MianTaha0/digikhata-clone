package com.digikhata.ui.navigation

object Routes {
    const val HOME = "home"
    const val SUPPLIER = "supplier"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val CREATE_BOOK = "createBook"
    const val BOOK_SETTINGS = "bookSettings/{bookId}"
    const val CLIENT_DETAIL = "clientDetail/{clientId}"
    const val COMING_SOON = "comingSoon/{label}"

    fun bookSettings(bookId: Long) = "bookSettings/$bookId"
    fun clientDetail(clientId: Long) = "clientDetail/$clientId"
    fun comingSoon(label: String) = "comingSoon/$label"
}
