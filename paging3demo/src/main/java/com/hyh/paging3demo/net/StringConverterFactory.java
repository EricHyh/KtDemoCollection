package com.hyh.paging3demo.net;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class StringConverterFactory extends Converter.Factory {

    @Override
    public Converter<ResponseBody, String> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
        if (String.class.equals(type)) {
            return ResponseBody::string;
            /*return new Converter<ResponseBody, String>(){

                @Override
                public String convert(ResponseBody value) throws IOException {
                    return value.string();
                }
            };*/
        }
        return null;
    }
}