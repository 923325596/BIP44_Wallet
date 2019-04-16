package com.lv.wallet;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.gson.Gson;
import com.lv.wallet.utils.SPUtils;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.HDUtils;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.utils.Numeric;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.novacrypto.bip39.MnemonicGenerator;
import io.github.novacrypto.bip39.Words;
import io.github.novacrypto.bip39.wordlists.English;

import static org.web3j.crypto.MnemonicUtils.generateSeed;

public class CreateWalletActivity extends AppCompatActivity {

    private TextView mMnemonic;
    private TextView mPrikey;
    private TextView mAddress;
    private TextView mKeystore;

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                Map<String,String> map = (Map<String, String>) msg.obj;
                String mnemonice = map.get("mnemonics");
                String prikey = map.get("prikey");
                String address = map.get("address");
                String keystore = map.get("keystore");

                mMnemonic.setText("助记词："+mnemonice);
                mPrikey.setText("私钥："+prikey);
                mAddress.setText("地址："+address);
                mKeystore.setText("Keystore："+keystore);
                SPUtils.put(CreateWalletActivity.this,"mnemonice",mnemonice);
                SPUtils.put(CreateWalletActivity.this,"prikey",prikey);
                SPUtils.put(CreateWalletActivity.this,"address",address);
                SPUtils.put(CreateWalletActivity.this,"keystore",keystore);
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_wallet);
        mMnemonic = findViewById(R.id.show_mnemonic);
        mPrikey = findViewById(R.id.show_prikey);
        mAddress = findViewById(R.id.show_address);
        mKeystore = findViewById(R.id.show_keystore);
    }


    public void cwallet(View view) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    createWallet();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public void createWallet() throws CipherException {
        Map<String, String> map = new HashMap<>();
        //1.通过bip44生成助记词
        StringBuilder sb = new StringBuilder();
        byte[] entropy = new byte[Words.TWELVE.byteLength()];
        new SecureRandom().nextBytes(entropy);
        new MnemonicGenerator(English.INSTANCE).createMnemonic(entropy, sb::append);
        String mnemonics = sb.toString();
        Log.e("+++", "生成的助记词：" + mnemonics);
        //2.生成种子
        byte[] seed = generateSeed(mnemonics, null);
        //3. 生成根私钥 root private key 树顶点的master key ；bip32
        DeterministicKey rootPrivateKey = HDKeyDerivation.createMasterPrivateKey(seed);
        // 4. 由根私钥生成 第一个HD 钱包
        DeterministicHierarchy dh = new DeterministicHierarchy(rootPrivateKey);
        // 5. 定义父路径 H则是加强 imtoken中的eth钱包进过测试发现使用的是此方式生成 bip44
        List<ChildNumber> parentPath = HDUtils.parsePath("M/44H/60H/0H/0");
        // 6. 由父路径,派生出第一个子私钥 "new ChildNumber(0)" 表示第一个 （m/44'/60'/0'/0/0）
        DeterministicKey child = dh.deriveChild(parentPath, true, true, new ChildNumber(0));
        byte[] privateKeyByte = child.getPrivKeyBytes();
        ECKeyPair keyPair = ECKeyPair.create(privateKeyByte);
        Log.e("TAG", "generateBip44Wallet: 钥匙对  私钥 = " + Numeric.toHexStringNoPrefix(keyPair.getPrivateKey()));
        Log.i("TAG", "generateBip44Wallet: 钥匙对  公钥 = " + Numeric.toHexStringNoPrefix(keyPair.getPublicKey()));
        WalletFile walletFile = Wallet.createLight("123456", keyPair);
        String address = Keys.toChecksumAddress(walletFile.getAddress());
        Log.e("lv", "address:" + address);
        Gson gson = new Gson();
        String keystore = gson.toJson(walletFile);
        Log.e("lv", "keystore:" + keystore);

        map.put("address", address);
        map.put("prikey", Numeric.toHexStringNoPrefix(keyPair.getPrivateKey()));
        map.put("mnemonics", mnemonics);
        map.put("keystore", keystore);

        Message message = Message.obtain();
        message.what = 0;
        message.obj = map;
        handler.sendMessage(message);

    }

}
