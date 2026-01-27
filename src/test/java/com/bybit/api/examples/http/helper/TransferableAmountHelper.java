package com.bybit.api.examples.http.helper;

import com.bybit.api.client.config.BybitApiConfig;
import com.bybit.api.client.domain.account.AccountType;
import com.bybit.api.client.domain.account.request.AccountDataRequest;
import com.bybit.api.client.service.BybitApiClientFactory;
import com.bybit.api.examples.http.dto.TransferableBalanceDto;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.LinkedHashMap;

@Slf4j
public class TransferableAmountHelper {
    
    private final BybitApiClientFactory httpFactory;
    
    public TransferableAmountHelper(String apiKey, String apiSecret, String baseUrl) {
        this.httpFactory = BybitApiClientFactory.newInstance(apiKey, apiSecret, baseUrl);
    }
    
    /**
     * Get transferable balance for a specific coin in the UNIFIED account.
     * This method demonstrates how to extract and parse the response from the getTransferableAmount endpoint.
     * 
     * @param coin The coin symbol (e.g., "USDT", "BTC", "ETH")
     * @return TransferableBalanceDto containing the transferable balance and timestamp
     */
    public TransferableBalanceDto getTransferableBalance(String coin) {
        log.info("Retrieving transferable amount for coin: {}", coin);
        
        AccountDataRequest request = AccountDataRequest.builder()
                .accountType(AccountType.UNIFIED)
                .coin(coin)
                .build();
        
        LinkedHashMap<String, Object> transferableAmountMap = 
            (LinkedHashMap<String, Object>) httpFactory.newAccountRestClient().getTransferableAmount(request);
        
        Long time = (Long) transferableAmountMap.get("time");
        if (time == null) {
            throw new RuntimeException("No 'time' field in response");
        }
        
        // Extract the list from the response
        LinkedHashMap<String, Object> result = (LinkedHashMap<String, Object>) transferableAmountMap.get("result");
        if (result == null) {
            throw new RuntimeException("No 'result' object in response");
        }
        
        ArrayList<LinkedHashMap<String, Object>> list = (ArrayList<LinkedHashMap<String, Object>>) result.get("list");
        if (list == null || list.isEmpty()) {
            throw new RuntimeException("No information in 'list' object");
        }
        
        // Get the first item from the list (should only be one for a specific coin query)
        LinkedHashMap<String, Object> data = list.get(0);
        String coinSymbol = (String) data.get("coin");
        
        if (coinSymbol == null || !coinSymbol.equals(coin)) {
            throw new RuntimeException("Coin mismatch: expected " + coin + " but got " + coinSymbol);
        }
        
        // Parse the transferable amount (it comes as a string from the API)
        String transferableAmountStr = (String) data.get("transferableAmount");
        if (transferableAmountStr == null || transferableAmountStr.isEmpty()) {
            throw new RuntimeException("No transferableAmount field in response for coin: " + coin);
        }
        
        Double transferableAmount;
        try {
            transferableAmount = Double.parseDouble(transferableAmountStr);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid transferableAmount value: " + transferableAmountStr, e);
        }
        
        log.info("Transferable amount for {}: {}", coin, transferableAmount);
        
        return new TransferableBalanceDto(transferableAmount, time);
    }
    
    public static void main(String[] args) {
        // Example usage
        String apiKey = "YOUR_API_KEY";
        String apiSecret = "YOUR_API_SECRET";
        
        TransferableAmountHelper helper = new TransferableAmountHelper(
            apiKey, 
            apiSecret, 
            BybitApiConfig.TESTNET_DOMAIN
        );
        
        try {
            TransferableBalanceDto balance = helper.getTransferableBalance("USDT");
            System.out.println("Transferable Balance: " + balance.getBalance());
            System.out.println("Timestamp: " + balance.getTime());
        } catch (Exception e) {
            System.err.println("Error retrieving transferable balance: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
