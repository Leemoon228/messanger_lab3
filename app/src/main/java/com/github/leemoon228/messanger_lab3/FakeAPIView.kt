package com.github.leemoon228.messanger_lab3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


@Composable
fun FakeAPIView (
    modifier: Modifier = Modifier
){
    LazyColumn(modifier) {

    }
}

@Composable
fun Post(
    post: FakeAPIPost,
    modifier: Modifier = Modifier
){
    Column(modifier.background(MaterialTheme.colorScheme.background )) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier =Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.id_d, post.id))
            Text(stringResource(R.string.user_id_d, post.userId))
        }
        Text(
            text = post.title,
            style =MaterialTheme.typography.titleMedium,
            modifier =Modifier.padding(4.dp)
        )
        Text(
            text = post.body,
            style =MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
@Preview
fun PostPreview() = Post(FakeAPIPost(1, 2, "Title", "Body"))