package com.lv.wallet;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lv.wallet.utils.SPUtils;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.Wallet;
import org.web3j.crypto.WalletFile;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;


/**
 * 作者：created by albert on 2019/4/16 16:15
 * 邮箱：lvzhongdi@icloud.com
 *
 * @param
 **/
public class TransferActivity extends AppCompatActivity {


    private EditText transferAddress;
    private EditText transferPin;
    private EditText transferValue;
    private Credentials mCredentials;
    private Web3j web3j = Web3jFactory.build(new HttpService("https://ropsten.infura.io/v3/b1a395a114ba485586c43d0fa227d443"));

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        initView();
    }

    private void initView() {
        transferAddress = findViewById(R.id.id_transfer_address);
        transferPin = findViewById(R.id.id_transfer_pin);
        transferValue = findViewById(R.id.id_transfer_value);
    }

    public void transferTo(View view) {
        String address = transferAddress.getText().toString();
        String pin = transferPin.getText().toString();
        String value = transferValue.getText().toString();
        if (TextUtils.isEmpty(address) || TextUtils.isEmpty(pin) || TextUtils.isEmpty(value)) {
            Toast.makeText(this, "填写内容不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    transfer(address, pin, value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * 转账
     *
     * @param toAddress
     * @param
     */
    private void transfer(String toAddress, String pin, String value) throws Exception {
        String keystore = SPUtils.get(TransferActivity.this, "keystore", null).toString();
        String fromAddress = SPUtils.get(TransferActivity.this, "address", null).toString();

        //切换到Ropsten环境

        ObjectMapper om = new ObjectMapper();
        WalletFile walletFile = om.readValue(keystore, WalletFile.class);
        mCredentials = Credentials.create(Wallet.decrypt(pin, walletFile));

        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                fromAddress, DefaultBlockParameterName.LATEST).sendAsync().get();

        BigInteger nonce = ethGetTransactionCount.getTransactionCount();

        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce, Convert.toWei("18", Convert.Unit.GWEI).toBigInteger(),
                Convert.toWei("45000", Convert.Unit.WEI).toBigInteger(), toAddress, new BigInteger(value));

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, mCredentials);

        String hexValue = Numeric.toHexString(signedMessage);

        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).sendAsync().get();

        if (ethSendTransaction.hasError()) {
            Log.e("+++transfer error:", ethSendTransaction.getError().getMessage());
        } else {
            String transactionHash = ethSendTransaction.getTransactionHash();
            Log.e("+++transactionHash:", "" + transactionHash);
        }

    }


    /**
     * 查询余额
     *
     * @param view
     */
    public void balance(View view) {
        String address = SPUtils.get(TransferActivity.this, "address", null).toString();
        new Thread() {
            @Override
            public void run() {
                super.run();
                searchBlance(address);
            }
        }.start();
    }


    private void searchBlance(String address) {
        //查询余额
        try {
            EthGetBalance ethGetBalance = web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            Log.e("+++", "balance:" + Convert.fromWei(ethGetBalance.getBalance().toString(), Convert.Unit.ETHER));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
