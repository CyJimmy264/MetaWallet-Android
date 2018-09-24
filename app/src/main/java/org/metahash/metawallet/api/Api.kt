package org.metahash.metawallet.api

import io.reactivex.Observable
import okhttp3.ResponseBody
import org.metahash.metawallet.data.models.LoginResponse
import org.metahash.metawallet.data.models.RefreshResponse
import org.metahash.metawallet.data.models.ServiceRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface Api {

    @POST
    fun login(@Url url: String, @Body body: ServiceRequest): Observable<LoginResponse>

    @POST
    fun getUserWallets(@Url url: String, @Body body: ServiceRequest): Observable<Response<ResponseBody>>

    @POST
    fun getWalletBalance(@Url url: String, @Body body: ServiceRequest): Observable<Response<ResponseBody>>

    @POST
    fun refreshToken(@Url url: String, @Body body: ServiceRequest): Observable<RefreshResponse>

    @POST
    fun getWalletHistory(@Url url: String, @Body body: ServiceRequest): Observable<Response<ResponseBody>>
}