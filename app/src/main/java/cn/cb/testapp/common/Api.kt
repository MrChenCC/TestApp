package cn.cb.testapp.common

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface Api {
    @Multipart
    @POST("api/uploadFile")
    fun uploadFiles(
        @Header("Authorization") authorization: String = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJsb2dpblR5cGUiOiJsb2dpbiIsImxvZ2luSWQiOiJzeXNfdXNlcjoxNzkxMzUwMjkzMjE2MTYxNzk0Iiwicm5TdHIiOiJ4dEhYd2VTTHpIdXZvNGhuaFh6dFdqZ2NlRXIxTm5RWiIsInVzZXJJZCI6IjE3OTEzNTAyOTMyMTYxNjE3OTQifQ.gFeGBcbrxgCN2CLm60Utj6cZgHzrpAZaS9Q1krfLg1w",
        @Part files: MultipartBody.Part
    ): Call<ResponseBody>
}