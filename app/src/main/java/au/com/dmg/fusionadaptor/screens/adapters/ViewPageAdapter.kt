package au.com.dmg.fusionadaptor.screens.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import au.com.dmg.fusionadaptor.screens.fragments.StatusFragment
import au.com.dmg.fusionadaptor.screens.fragments.SettingsFragment
import au.com.dmg.fusionadaptor.screens.fragments.DevelopmentFragment

class ViewPagerAdapter(fragmentManager: FragmentManager,lifecycle: Lifecycle): FragmentStateAdapter(fragmentManager, lifecycle) {
    override fun getItemCount(): Int {
        return 3
    }

    override fun createFragment(position: Int): Fragment {
        return   when(position){
            0->{
                StatusFragment()
            }
            1->{
                SettingsFragment()
            }
            2->{
                DevelopmentFragment()
            }
            else->{
                Fragment()
            }

        }
    }
}