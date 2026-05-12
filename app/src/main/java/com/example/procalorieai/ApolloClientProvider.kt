// ApolloClientProvider.kt — добавляем авторефреш токена
package com.procalorieai

import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import kotlinx.coroutines.runBlocking

object ApolloClientProvider {
    // Эмулятор: 10.0.2.2, реальный девайс: IP твоей машины
    const val BASE_URL = "http://10.0.2.2:8000/graphql"
    private const val TAG = "ApolloClient"
    private var _apolloClient: ApolloClient? = null

    fun getApolloClient(): ApolloClient {
        if (_apolloClient == null) {
            _apolloClient = buildClient()
        }
        return _apolloClient!!
    }

    fun refreshClient() {
        _apolloClient = null
    }

    private fun buildClient(): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(BASE_URL)
            .addHttpInterceptor(AuthInterceptor())
            .build()
    }

    // Перехватчик: добавляет токен и авторефрешит при 401
    private class AuthInterceptor : HttpInterceptor {
        override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
            val token = TokenManager.getAccessToken()
            val authedRequest = request.newBuilder()
                .apply { if (token != null) addHeader("Authorization", "Bearer $token") }
                .build()

            val response = chain.proceed(authedRequest)

            // Если 401 — пробуем рефрешнуть
            if (response.statusCode == 401) {
                val refreshed = tryRefreshToken()
                if (refreshed) {
                    val newToken = TokenManager.getAccessToken()
                    val retryRequest = request.newBuilder()
                        .apply { if (newToken != null) addHeader("Authorization", "Bearer $newToken") }
                        .build()
                    return chain.proceed(retryRequest)
                }
            }
            return response
        }

        // ApolloClientProvider.kt — только блок tryRefreshToken
        private fun tryRefreshToken(): Boolean {
            val refreshToken = TokenManager.getRefreshToken() ?: return false
            return try {
                val client = ApolloClient.Builder().serverUrl(BASE_URL).build()
                val response = runBlocking {
                    client.mutation(
                        com.procalorieai.RefreshTokensMutation(
                            input = com.procalorieai.type.RefreshInput(
                                refreshToken = refreshToken
                            )
                        )
                    ).execute()
                }
                val data = response.data?.refreshTokens
                if (data != null) {
                    TokenManager.saveTokens(data.accessToken, data.refreshToken)
                    true
                } else false
            } catch (e: Exception) {
                TokenManager.clearTokens()
                false
            }
        }
    }
}