#include <jni.h>
#include <string>
#include "aes.h"
#include "md5.h"
#include <sys/ptrace.h>
#include <unistd.h>
#include "android_log.h"

static const uint8_t AES_KEY[] = "@1p!7@n?w%4^L&u*Q(9)R+g#h-B_0=Z;";
static const uint8_t AES_IV[] = "p3?T@6^r!L+8#n)_";
static const string PWD_MD5_KEY = "4J9lKuR2c8OuDPBAniEy5USFQdSM0An4";


uint8_t *jstringTostring(JNIEnv *env, jstring jstr) {
    uint8_t *rtn = nullptr;
    jclass clsstring = env->FindClass("java/lang/String");
    jstring strencode = env->NewStringUTF("utf-8");
    jmethodID mid = env->GetMethodID(clsstring, "getBytes", "(Ljava/lang/String;)[B");
    auto barr = (jbyteArray) env->CallObjectMethod(jstr, mid, strencode);
    jsize alen = env->GetArrayLength(barr);
    jbyte *ba = env->GetByteArrayElements(barr, JNI_FALSE);
    if (alen > 0) {
        rtn = (uint8_t *) malloc(alen + 1);
        memcpy(rtn, ba, alen);
        rtn[alen] = 0;
    }
    env->ReleaseByteArrayElements(barr, ba, 0);
    return rtn;
}

int key[] = {1, 2, 3, 4, 5};//加密字符密钥

//异或
void xor_go(char *pstr, const int *pkey) {
    int len = strlen(pstr);//获取长度
    for (int i = 0; i < len; i++) {
        *(pstr + i) = ((*(pstr + i)) ^ (pkey[i % 5]));
    }
}

extern "C"
JNIEXPORT jstring JNICALL
Java_x_f_alpha(JNIEnv *env, jclass type, jbyteArray jbArr) {

    char *str;
    jsize alen = env->GetArrayLength(jbArr);
    jbyte *ba = env->GetByteArrayElements(jbArr, JNI_FALSE);
    str = (char *) malloc(alen + 1);
    memcpy(str, ba, alen);
    str[alen] = '\0';
    env->ReleaseByteArrayElements(jbArr, ba, 0);

    // char *result = AES_ECB_PKCS7_Encrypt(str, AES_KEY);//AES ECB PKCS7Padding加密
    char *result = AES_CBC_PKCS7_Encrypt(str, AES_KEY, AES_IV);//AES CBC PKCS7Padding加密
    return env->NewStringUTF(result);
}

extern "C"
JNIEXPORT jbyteArray JNICALL
Java_x_f_delta(JNIEnv *env, jclass type, jstring out_str) {

    const char *str = env->GetStringUTFChars(out_str, nullptr);
    // char *result = AES_ECB_PKCS7_Decrypt(str, AES_KEY);//AES ECB PKCS7Padding解密
    char *result = AES_CBC_PKCS7_Decrypt(str, AES_KEY, AES_IV);//AES CBC PKCS7Padding解密
    env->ReleaseStringUTFChars(out_str, str);

    auto len = (jsize) strlen(result);
    jbyteArray jbArr = env->NewByteArray(len);
    env->SetByteArrayRegion(jbArr, 0, len, (jbyte *) result);
    return jbArr;
}