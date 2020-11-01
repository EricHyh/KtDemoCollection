package com.hyh.dialog.account

class AccountGroup(val brokerId: Int, val accounts: List<AccountData>)

data class AccountData(
    val brokerId: Int,
    val account: AccountType,
    val accountId: Long
)