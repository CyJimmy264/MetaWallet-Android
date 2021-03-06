package org.metahash.metawallet.api.mappers

import org.metahash.metawallet.data.models.WalletDataSimple
import org.metahash.metawallet.data.models.WalletsData

class WalletWithBalanceMapper : BaseMapper<WalletsData, WalletDataSimple>() {

    override fun fromEntity(from: WalletsData) =
        WalletDataSimple(
            from.address,
            from.name ?: "",
            from.balance.balance.toString(),
            from.hasPrivateKey,
            from.balance.delegatedBy.toString()
        )
}