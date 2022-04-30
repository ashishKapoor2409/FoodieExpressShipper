package com.example.android.foodieexpressshipper.ui.home

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.LayoutAnimationController
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.foodieexpressshipper.Adapter.MyShippingOrderAdapter
import com.example.android.foodieexpressshipper.R
import com.example.android.foodieexpressshipper.common.Common
import com.example.android.foodieexpressshipper.model.ShipperOrderModel

class HomeFragment : Fragment() {

    companion object {
        fun newInstance() = HomeFragment()
    }

    var layoutAnimationController : LayoutAnimationController? = null
    var adapter:MyShippingOrderAdapter? = null
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var recycler_order: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.home_fragment, container, false)
        recycler_order = root.findViewById(R.id.recycler_order)
        initViews(root)
        homeViewModel!!.messageError.observe(viewLifecycleOwner, Observer {s: String ->
            Toast.makeText(context,s,Toast.LENGTH_SHORT).show() })
        homeViewModel!!.getOrderModelMutableLiveData(Common.currentShipperUser!!.phone!!)
            .observe(viewLifecycleOwner, Observer { shippingOrderModels:List<ShipperOrderModel> ->
                adapter = MyShippingOrderAdapter(requireContext(),shippingOrderModels)
                recycler_order!!.adapter = adapter
                recycler_order!!.layoutAnimation = layoutAnimationController
            })
        return root
    }

    private fun initViews(root: View?) {
        recycler_order.setHasFixedSize(true)
        recycler_order!!.layoutManager = LinearLayoutManager(context)
        layoutAnimationController = AnimationUtils.loadLayoutAnimation(context,R.anim.layout_item_from_left)
    }

}