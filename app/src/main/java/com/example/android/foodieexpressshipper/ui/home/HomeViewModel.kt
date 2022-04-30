package com.example.android.foodieexpressshipper.ui.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.android.foodieexpressshipper.callback.IShippingOrderCallbackListener
import com.example.android.foodieexpressshipper.common.Common
import com.example.android.foodieexpressshipper.model.ShipperOrderModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HomeViewModel : ViewModel(), IShippingOrderCallbackListener {
    private val orderModelMutableLiveData : MutableLiveData<List<ShipperOrderModel>>
    val messageError: MutableLiveData<String>
    private val listener: IShippingOrderCallbackListener

    init{
        orderModelMutableLiveData = MutableLiveData()
        messageError = MutableLiveData()
        listener = this
    }

    fun getOrderModelMutableLiveData(shipperPhone:String): MutableLiveData<List<ShipperOrderModel>> {
        loadOrderByShipper(shipperPhone)
        return orderModelMutableLiveData
    }

    private fun loadOrderByShipper(shipperPhone: String) {
        val tempList : MutableList<ShipperOrderModel> = ArrayList()
        val orderRef = FirebaseDatabase.getInstance()
            .getReference(Common.SHIPPING_ORDER_REF)
            .orderByChild("shipperPhone")
            .equalTo(Common.currentShipperUser!!.phone)

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(p0: DataSnapshot) {
                for(itemSnapShot in p0.children) {
                    val shippingOrder = itemSnapShot.getValue(ShipperOrderModel::class.java)
                    tempList.add(shippingOrder!!)
                }
                listener.onShippingOrderLoadSuccess(tempList)
            }

            override fun onCancelled(p0: DatabaseError) {
                listener.onShippingOrderLoadFailed(p0.message)
            }

        })



    }

    override fun onShippingOrderLoadSuccess(shippingOrders: List<ShipperOrderModel>) {
        orderModelMutableLiveData.value = shippingOrders
    }

    override fun onShippingOrderLoadFailed(message: String) {
        messageError.value = message
    }
}