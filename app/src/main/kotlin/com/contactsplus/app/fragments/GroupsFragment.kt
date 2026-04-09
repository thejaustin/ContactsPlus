package com.contactsplus.app.fragments

import android.content.Context
import android.util.AttributeSet
import org.fossify.commons.helpers.TAB_GROUPS
import com.contactsplus.app.activities.MainActivity
import com.contactsplus.app.activities.SimpleActivity
import com.contactsplus.app.databinding.FragmentGroupsBinding
import com.contactsplus.app.databinding.FragmentLayoutBinding
import com.contactsplus.app.dialogs.CreateNewGroupDialog

class GroupsFragment(context: Context, attributeSet: AttributeSet) : MyViewPagerFragment<MyViewPagerFragment.FragmentLayout>(context, attributeSet) {

    private lateinit var binding: FragmentGroupsBinding

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = FragmentGroupsBinding.bind(this)
        innerBinding = FragmentLayout(FragmentLayoutBinding.bind(binding.root))
    }

    override fun fabClicked() {
        finishActMode()
        showNewGroupsDialog()
    }

    override fun placeholderClicked() {
        showNewGroupsDialog()
    }

    private fun showNewGroupsDialog() {
        CreateNewGroupDialog(activity as SimpleActivity) {
            (activity as? MainActivity)?.refreshContacts(TAB_GROUPS)
        }
    }
}
