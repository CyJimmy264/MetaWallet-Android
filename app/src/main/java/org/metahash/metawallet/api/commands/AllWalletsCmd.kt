package org.metahash.metawallet.api.commands

import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.metahash.metawallet.Constants
import org.metahash.metawallet.WalletApplication
import org.metahash.metawallet.api.Api
import org.metahash.metawallet.api.ServiceRequestFactory
import org.metahash.metawallet.api.base.BaseCommand
import org.metahash.metawallet.api.mappers.LocalWalletToWalletMapper
import org.metahash.metawallet.api.mappers.WalletWithBalanceMapper
import org.metahash.metawallet.data.models.BalanceData
import org.metahash.metawallet.data.models.BalanceResponse
import org.metahash.metawallet.data.models.WalletsData
import java.util.concurrent.Executors

class AllWalletsCmd(
        private val api: Api,
        private val balanceCmd: WalletBalanceCmd
) : BaseCommand<List<WalletsData>>() {

    private val executor = Executors.newFixedThreadPool(3)
    private val toSimpleWalletMapper = WalletWithBalanceMapper()
    private val fromLocalMapper = LocalWalletToWalletMapper()

    var currency = ""
    var isOnlyLocal = false

    override fun serviceRequest(): Observable<List<WalletsData>> {
        return getWalletsRequest()
                .map {
                    val local = WalletApplication.dbHelper.getUserWalletsByCurrency(currency)
                    val result = it.data.toMutableList()
                    //remove all local wallet from remote
                    local.forEach { wallet ->
                        result.removeAll { it.address.equals(wallet.address, true) }
                    }
                    result.addAll(local.map { fromLocalMapper.fromEntity(it) })
                    result
                }
                .flatMap(
                        { getBalancesRequest(it.map { it.address }) },
                        { wallets, balances ->
                            wallets.forEach { wallet ->
                                val balance = balances.firstOrNull { it.address == wallet.address }
                                if (balance != null) {
                                    wallet.balance = balance
                                }
                            }
                            wallets
                        })
    }

    private fun getBalancesRequest(addresses: List<String>): Observable<List<BalanceData>> {
        val list = mutableListOf<Observable<BalanceResponse>>()
        addresses.forEach {
            balanceCmd.address = it
            balanceCmd.subscribeScheduler = Schedulers.from(executor)
            list.add(balanceCmd.execute())
        }

        if (addresses.isEmpty()) {
            return Observable.just(listOf())
        }

        return Observable.combineLatest(list)
        { balances ->
            balances.map {
                it as BalanceResponse
                it.result
            }
        }
    }

    fun getWalletsRequest() = api
            .getUserWallets(Constants.BASE_URL_WALLET,
                    ServiceRequestFactory.getRequestData(
                            ServiceRequestFactory.REQUESTTYPE.ALLWALLETS,
                            ServiceRequestFactory.getAllWalletsParams(currency)))

    fun executeWithCache() = execute()
            .observeOn(Schedulers.computation())
            .doOnNext {
                if (it.isNotEmpty()) {
                    WalletApplication.dbHelper.setWallets(it, currency)
                }
            }
            .startWith(Observable.fromCallable { WalletApplication.dbHelper.getWallets(currency) }
                    .subscribeOn(Schedulers.computation())
                    .filter { it.isNotEmpty() }
            )
            //filter by isLocalOnly variable
            .map {
                if (isOnlyLocal.not()) {
                    it
                } else {
                    it.filter { it.hasPrivateKey }
                }
            }
            .map { it.map { toSimpleWalletMapper.fromEntity(it) } }
            .map { WalletApplication.gson.toJson(it) }
}