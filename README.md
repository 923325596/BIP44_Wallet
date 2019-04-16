# BIP44_Wallet
学习创建一个以太坊钱包，BIP44 钱包
1.引用库
web3j:
implementation 'org.web3j:core:3.3.1-android'
bitcoinj: 
implementation 'org.bitcoinj:bitcoinj-core:0.14.7'

//我的Gradle文件如下：
´´´
android {
    compileSdkVersion 27
    defaultConfig {
        applicationId "com.lv.wallet"
        minSdkVersion 19
        targetSdkVersion 27
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
    compileOptions {
        targetCompatibility 1.8
        sourceCompatibility 1.8
    }
}

dependencies {
    implementation fileTree(include: ['*.jar'], dir: 'libs')
    implementation 'com.android.support:appcompat-v7:27.1.1'
    implementation 'com.android.support.constraint:constraint-layout:1.1.3'
    //web3j lib
    implementation 'org.web3j:core:3.3.1-android'
    implementation 'org.bitcoinj:bitcoinj-core:0.14.7'
    //用于生成助记词
    implementation 'io.github.novacrypto:BIP39:0.1.9'
    implementation 'com.google.code.gson:gson:2.8.5'
}
´´´
在MainActivity中分别实现了创建钱包。导入钱包功能。
2018年11月12日23:38:36 后续会继续更新钱包转账，查询等功能
