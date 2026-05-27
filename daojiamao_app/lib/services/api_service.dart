import 'package:dio/dio.dart';
import 'package:get/get.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiService extends GetxService {
  late Dio _dio;
  String _baseUrl = 'http://192.168.1.100:5000';

  String get baseUrl => _baseUrl;

  Future<ApiService> init() async {
    final prefs = await SharedPreferences.getInstance();
    _baseUrl = prefs.getString('server_url') ?? _baseUrl;

    _dio = Dio(BaseOptions(
      baseUrl: _baseUrl,
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 30),
      headers: {'Content-Type': 'application/json'},
    ));

    _dio.interceptors.add(LogInterceptor(
      requestBody: true,
      responseBody: true,
    ));

    return this;
  }

  void updateBaseUrl(String url) {
    _baseUrl = url;
    _dio.options.baseUrl = url;
    SharedPreferences.getInstance().then((prefs) {
      prefs.setString('server_url', url);
    });
  }

  // Cookie验证
  Future<bool> validateCookie() async {
    try {
      final res = await _dio.get('/api/validate_cookie');
      return res.data['data']?['valid'] == true;
    } catch (e) {
      return false;
    }
  }

  // Cookie状态
  Future<Map> getCookieStatus() async {
    try {
      final res = await _dio.get('/api/cookie/status');
      return res.data;
    } catch (e) {
      return {'code': -1, 'message': '获取Cookie状态失败'};
    }
  }

  // 更新Cookie
  Future<bool> updateCookie(String content) async {
    try {
      final res = await _dio.post('/api/cookie/update_text', data: {'content': content});
      return res.data['code'] == 0;
    } catch (e) {
      return false;
    }
  }

  // 查询手机号
  Future<String?> queryMobileByOrder(String orderId) async {
    try {
      final res = await _dio.get('/api/query_mobile/$orderId');
      if (res.data['code'] == 0) return res.data['data']?['mobile'];
    } catch (e) {}
    return null;
  }

  // 查询订单
  Future<Map> queryOrders({String? mobile, String? sellerMobile, int page = 1}) async {
    try {
      final res = await _dio.post('/api/query_orders', data: {
        'mobile': mobile,
        'seller_mobile': sellerMobile,
        'page': page,
        'page_size': 20,
      });
      return res.data;
    } catch (e) {
      return {'code': -1, 'message': '查询订单失败'};
    }
  }

  // 查询套餐
  Future<List> queryCombos(String mobile) async {
    try {
      final res = await _dio.get('/api/query_combos/$mobile');
      if (res.data['code'] == 0) return res.data['data']?['combos'] ?? [];
    } catch (e) {}
    return [];
  }

  // 查询套餐地址
  Future<List> getComboAddresses(String comboId) async {
    try {
      final res = await _dio.get('/api/combo_addresses/$comboId');
      if (res.data['code'] == 0) return res.data['data']?['addresses'] ?? [];
    } catch (e) {}
    return [];
  }

  // 添加地址
  Future<String?> addAddress(String comboId, String address, String location, String mobile) async {
    try {
      final res = await _dio.post('/api/add_address', queryParameters: {
        'combo_id': comboId,
        'address': address,
        'location': location,
        'mobile': mobile,
      });
      if (res.data['code'] == 0) return res.data['data']?['service_info_id'];
    } catch (e) {}
    return null;
  }

  // 下单-单次
  Future<Map> placeSingleOrder({
    required String mobile,
    required String comboId,
    required String serviceInfoId,
    required String sellerId,
    required String serviceTime,
  }) async {
    try {
      final res = await _dio.post('/api/place_single_order', data: {
        'mobile': mobile,
        'combo_id': comboId,
        'service_info_id': serviceInfoId,
        'seller_id': sellerId,
        'service_time': serviceTime,
      });
      return res.data;
    } catch (e) {
      return {'code': -1, 'message': '下单失败'};
    }
  }

  // 下单-周期
  Future<Map> placeCycleOrder({
    required String mobile,
    required String comboId,
    required String serviceInfoId,
    required String sellerId,
    required int weekType,
    required String serverTimeCycles,
    required String beginServerTime,
  }) async {
    try {
      final res = await _dio.post('/api/place_cycle_order', data: {
        'mobile': mobile,
        'combo_id': comboId,
        'service_info_id': serviceInfoId,
        'seller_id': sellerId,
        'week_type': weekType,
        'server_time_cycles': serverTimeCycles,
        'begin_server_time': beginServerTime,
      });
      return res.data;
    } catch (e) {
      return {'code': -1, 'message': '下单失败'};
    }
  }

  // 搜索保洁师
  Future<List> searchSellers({
    int serviceId = 1,
    String? serviceTime,
    int duration = 4,
    String? location,
    String? address,
    int distance = 7,
  }) async {
    try {
      final res = await _dio.get('/api/search_sellers', queryParameters: {
        'service_id': serviceId,
        'service_time': serviceTime,
        'duration': duration,
        'location': location,
        'address': address,
        'distance': distance,
      });
      if (res.data['code'] == 0) return res.data['data']?['sellers'] ?? [];
    } catch (e) {}
    return [];
  }

  // 查询保洁师
  Future<String?> getSellerByMobile(String mobile) async {
    try {
      final res = await _dio.get('/api/get_seller/$mobile');
      if (res.data['code'] == 0) return res.data['data']?['seller_id'];
    } catch (e) {}
    return null;
  }

  // 地理编码
  Future<Map> geocode(String address) async {
    try {
      final res = await _dio.get('/api/geocode', queryParameters: {'address': address});
      return res.data;
    } catch (e) {
      return {'code': -1};
    }
  }

  // 现金结算信息
  Future<Map> getOrderCashInfo(String orderId) async {
    try {
      final res = await _dio.get('/api/order_cash_info/$orderId');
      return res.data;
    } catch (e) {
      return {'code': -1, 'message': '查询失败'};
    }
  }

  // 现金结算
  Future<bool> cashPay(String orderId, double amount) async {
    try {
      final res = await _dio.post('/api/cash_pay', data: {
        'order_id': orderId,
        'amount': amount,
      });
      return res.data['code'] == 0;
    } catch (e) {
      return false;
    }
  }
}
