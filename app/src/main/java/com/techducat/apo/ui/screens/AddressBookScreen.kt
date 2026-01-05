package com.techducat.apo.ui.screens
import com.techducat.apo.ui.theme.MoneroWalletTheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techducat.apo.ui.components.EmptyState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import com.techducat.apo.models.AddressBookEntry
import com.techducat.apo.storage.WalletDataStore
import com.techducat.apo.ui.components.AddressBookCard
import com.techducat.apo.R

// ============================================================================
// ADDRESSBOOKSCREEN
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressBookScreen(
    dataStore: WalletDataStore,
    onSelectAddress: (String) -> Unit = {},
    onClose: () -> Unit = {}
) {
    var entries by remember { mutableStateOf(dataStore.loadAddressBook()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<AddressBookEntry?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<AddressBookEntry?>(null) }
    val scope = rememberCoroutineScope()
    
    val filteredEntries = remember(entries, searchQuery) {
        entries.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.notes.contains(searchQuery, ignoreCase = true)
        }.sortedWith(
            compareByDescending<AddressBookEntry> { it.isFavorite }
                .thenByDescending { it.dateAdded }
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.address_book), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.menu_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, stringResource(R.string.add_contact))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { stringResource(R.string.search_contacts) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )
            
            if (filteredEntries.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.ContactPage,
                    title = if (searchQuery.isNotEmpty()) stringResource(R.string.address_book_no_matching_contacts) else stringResource(R.string.no_contacts),
                    message = if (searchQuery.isNotEmpty()) 
                        stringResource(R.string.address_book_try_a_different_search)
                    else 
                        stringResource(R.string.add_first_contact)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredEntries) { entry ->
                        AddressBookCard(
                            entry = entry,
                            onEdit = { selectedEntry = entry },
                            onDelete = {
                                entryToDelete = entry
                                showDeleteConfirm = true
                            },
                            onToggleFavorite = {
                                scope.launch {
                                    entries = entries.map {
                                        if (it.id == entry.id) it.copy(isFavorite = !it.isFavorite) else it
                                    }
                                    dataStore.saveAddressBook(entries)
                                }
                            },
                            onSelect = { onSelectAddress(entry.address) }
                        )
                    }
                }
            }
        }
    }
}
