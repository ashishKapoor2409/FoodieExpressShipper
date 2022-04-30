package com.example.android.foodieexpressshipper.callback

import com.example.android.foodieexpressshipper.model.ShipperOrderModel

interface IShippingOrderCallbackListener {
    fun onShippingOrderLoadSuccess(shippingOrders: List<ShipperOrderModel>)
    fun onShippingOrderLoadFailed(message: String)
}