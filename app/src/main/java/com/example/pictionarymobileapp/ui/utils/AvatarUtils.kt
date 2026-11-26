package com.example.pictionarymobileapp.ui.utils

import com.example.pictionarymobileapp.R

// Returns the drawable resource ID corresponding to the given avatar ID
fun getAvatarDrawableResId(avatarId: Int): Int {
    return when (avatarId) {
        1 -> R.drawable.avatar_1
        2 -> R.drawable.avatar_2
        3 -> R.drawable.avatar_3
        4 -> R.drawable.avatar_4
        5 -> R.drawable.avatar_5
        6 -> R.drawable.avatar_6
        7 -> R.drawable.avatar_7
        8 -> R.drawable.avatar_8
        else -> R.drawable.avatar_1
    }
}
