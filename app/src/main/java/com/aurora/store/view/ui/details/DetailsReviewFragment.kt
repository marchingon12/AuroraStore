/*
 * Aurora Store
 *  Copyright (C) 2021, Rahul Kumar Patel <whyorean@gmail.com>
 *
 *  Aurora Store is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Aurora Store is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.aurora.store.view.ui.details

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.aurora.gplayapi.data.models.Review
import com.aurora.gplayapi.data.models.ReviewCluster
import com.aurora.store.R
import com.aurora.store.databinding.FragmentDetailsReviewBinding
import com.aurora.store.view.custom.recycler.EndlessRecyclerOnScrollListener
import com.aurora.store.view.epoxy.views.AppProgressViewModel_
import com.aurora.store.view.epoxy.views.details.ReviewViewModel_
import com.aurora.store.viewmodel.review.ReviewViewModel

class DetailsReviewFragment : Fragment(R.layout.fragment_details_review) {

    private var _binding: FragmentDetailsReviewBinding? = null
    private val binding: FragmentDetailsReviewBinding
        get() = _binding!!

    private val args: DetailsReviewFragmentArgs by navArgs()

    private lateinit var VM: ReviewViewModel

    private lateinit var endlessRecyclerOnScrollListener: EndlessRecyclerOnScrollListener

    private lateinit var filter: Review.Filter
    private lateinit var reviewCluster: ReviewCluster

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDetailsReviewBinding.bind(view)
        VM = ViewModelProvider(this)[ReviewViewModel::class.java]

        // Toolbar
        binding.layoutToolbarActionReview.apply {
            txtTitle.text = args.displayName
            toolbar.setOnClickListener {
                findNavController().navigateUp()
            }
        }

        VM.liveData.observe(viewLifecycleOwner) {
            if (!::reviewCluster.isInitialized) {
                endlessRecyclerOnScrollListener = object : EndlessRecyclerOnScrollListener() {
                    override fun onLoadMore(currentPage: Int) {
                        if (::reviewCluster.isInitialized) {
                            VM.next(reviewCluster.nextPageUrl)
                        }
                    }
                }
                binding.recycler.addOnScrollListener(endlessRecyclerOnScrollListener)
            }

            it?.let {
                reviewCluster = it
                updateController(reviewCluster)
            }
        }

        // Fetch Reviews
        filter = Review.Filter.ALL
        VM.fetchReview(args.packageName, filter)

        // Chips
        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds[0]) {
                R.id.filter_review_all -> filter = Review.Filter.ALL
                R.id.filter_review_critical -> filter = Review.Filter.CRITICAL
                R.id.filter_review_positive -> filter = Review.Filter.POSITIVE
                R.id.filter_review_five -> filter = Review.Filter.FIVE
                R.id.filter_review_four -> filter = Review.Filter.FOUR
                R.id.filter_review_three -> filter = Review.Filter.THREE
                R.id.filter_review_two -> filter = Review.Filter.TWO
                R.id.filter_review_one -> filter = Review.Filter.ONE
            }

            endlessRecyclerOnScrollListener.resetPageCount()
            VM.fetchReview(args.packageName, filter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateController(reviewCluster: ReviewCluster) {
        binding.recycler.withModels {
            setFilterDuplicates(true)
            reviewCluster.reviewList.forEach {
                add(
                    ReviewViewModel_()
                        .id(it.commentId)
                        .review(it)
                )
            }

            if (reviewCluster.hasNext()) {
                add(
                    AppProgressViewModel_()
                        .id("progress")
                )
            }
        }
    }
}
