package com.sagrd.ramdomwomen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sagrd.ramdomwomen.ui.theme.RamdomWomenTheme
import com.sagrd.ramdomwomen.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RamdomWomenTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    CoinListScreen()
                }
            }
        }
    }
}

@Composable
fun CoinListScreen(
    viewModel: CoinViewModel = hiltViewModel()
) {

    val state = viewModel.state.value

    Column(modifier = Modifier.fillMaxSize()) {
         LazyColumn(modifier = Modifier.fillMaxSize()){
             items( state.coins){ coin ->
                  CoinItem(coin = coin, {})
             }
         }

        if (state.isLoading)
            CircularProgressIndicator()

    }

}

@Composable
fun CoinItem(
    coin:CoinDto,
    onClick : (CoinDto) -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick(coin) }
        .padding(16.dp)
    ) {
        Text(
            text = "${coin.name} (${coin.symbol})",
            style = MaterialTheme.typography.body1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = if(coin.is_active) "Activa" else "Inactiva",
            color = if(coin.is_active) Color.Green else Color.Red ,
            fontStyle = FontStyle.Italic,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.align(CenterVertically)
        )

    }

}

@Composable
fun CoinScreen(viewModel: CoinViewModel = hiltViewModel()) {
    //val coin = viewModel.coin.value

    Column(modifier = Modifier.fillMaxSize()) {
       /* Text(text = coin.name)
        Text(text = coin.symbol)*/
    }

}

//RUTA: data/remote/dto
data class CoinDto(
    val id: String = "",
    val name: String = "",
    val symbol: String = "",
    val rank: Int = 0,
    val is_new: Boolean = false,
    val is_active: Boolean = false,
    val type: String = ""
)

//RUTA: data/remote
interface CoinApi {
    @GET("/v1/coins")
    suspend fun getCoins(): List<CoinDto>

    @GET("/v1/coins/{coinId}")
    suspend fun getCoin(@Path("coinId") coinId: String): CoinDto
}

class CoinsRepository @Inject constructor(
    private val api: CoinApi
) {
    fun getCoins(): Flow<Resource<List<CoinDto>>> = flow {
        try {
            emit(Resource.Loading()) //indicar que estamos cargando

            val coins = api.getCoins() //descarga las monedas de internet, se supone quedemora algo

            emit(Resource.Success(coins)) //indicar que se cargo correctamente y pasarle las monedas
        } catch (e: HttpException) {
            //error general HTTP
            emit(Resource.Error(e.message ?: "Error HTTP GENERAL"))
        } catch (e: IOException) {
            //debe verificar tu conexion a internet
            emit(Resource.Error(e.message ?: "verificar tu conexion a internet"))
        }
    }
}

data class CoinListState(
    val isLoading: Boolean = false,
    val coins: List<CoinDto> = emptyList(),
    val error: String = ""
)

@HiltViewModel
class CoinViewModel @Inject constructor(
    private val coinsRepository: CoinsRepository
) : ViewModel() {

    private var _state = mutableStateOf(CoinListState())
    val state: State<CoinListState> = _state

    init {
        coinsRepository.getCoins().onEach { result ->
            when (result) {
                is Resource.Loading -> {
                    _state.value = CoinListState(isLoading = true)
                }

                is Resource.Success -> {
                    _state.value = CoinListState(coins = result.data ?: emptyList())
                }
                is Resource.Error -> {
                    _state.value = CoinListState(error = result.message ?: "Error desconocido")
                }
            }
        }.launchIn(viewModelScope)
    }

}