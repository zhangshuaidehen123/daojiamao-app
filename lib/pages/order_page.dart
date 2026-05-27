import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../services/api_service.dart';

class OrderPage extends StatefulWidget {
  const OrderPage({super.key});

  @override
  State<OrderPage> createState() => _OrderPageState();
}

class _OrderPageState extends State<OrderPage> {
  final _mobileController = TextEditingController();
  final _addressController = TextEditingController();
  final _sellerMobileController = TextEditingController();

  String? _selectedComboId;
  String? _selectedAddressId;
  String? _selectedSellerId;
  DateTime _selectedDate = DateTime.now();
  TimeOfDay _selectedTime = TimeOfDay.now();
  String _assignMode = 'random'; // random or assigned
  bool _isCycle = false;
  int _weekType = 1;

  List _combos = [];
  List _addresses = [];
  List _sellers = [];
  bool _loading = false;

  @override
  void dispose() {
    _mobileController.dispose();
    _addressController.dispose();
    _sellerMobileController.dispose();
    super.dispose();
  }

  Future<void> _queryCombos() async {
    if (_mobileController.text.isEmpty) {
      Get.snackbar('提示', '请输入手机号');
      return;
    }

    setState(() => _loading = true);
    final api = Get.find<ApiService>();
    final result = await api.queryCombos(_mobileController.text);
    setState(() {
      _combos = result;
      _loading = false;
      if (_combos.isNotEmpty) {
        _selectedComboId = _combos[0]['comboOrderId'];
        _loadAddresses();
      }
    });
  }

  Future<void> _loadAddresses() async {
    if (_selectedComboId == null) return;

    final api = Get.find<ApiService>();
    final result = await api.getComboAddresses(_selectedComboId!);
    setState(() {
      _addresses = result;
      if (_addresses.isNotEmpty) {
        _selectedAddressId = _addresses[0]['id']?.toString();
      }
    });
  }

  Future<void> _searchSellers() async {
    if (_addressController.text.isEmpty) {
      Get.snackbar('提示', '请先输入地址');
      return;
    }

    setState(() => _loading = true);
    final api = Get.find<ApiService>();

    // 先地理编码获取坐标
    final geoResult = await api.geocode(_addressController.text);
    String? location;
    if (geoResult['code'] == 0) {
      location = geoResult['data']?['location'];
    }

    final serviceTime = '${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')} ${_selectedTime.hour.toString().padLeft(2, '0')}:${_selectedTime.minute.toString().padLeft(2, '0')}';

    final result = await api.searchSellers(
      serviceTime: serviceTime,
      location: location,
      address: _addressController.text,
    );

    setState(() {
      _sellers = result;
      _loading = false;
      if (_sellers.isNotEmpty) {
        _selectedSellerId = _sellers[0]['seller_id'];
      }
    });
  }

  Future<void> _placeOrder() async {
    if (_mobileController.text.isEmpty || _selectedComboId == null ||
        _selectedAddressId == null || _selectedSellerId == null) {
      Get.snackbar('提示', '请完善下单信息');
      return;
    }

    setState(() => _loading = true);

    final api = Get.find<ApiService>();
    final serviceTime = '${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')} ${_selectedTime.hour.toString().padLeft(2, '0')}:${_selectedTime.minute.toString().padLeft(2, '0')}';

    Map result;
    if (_isCycle) {
      // 周期单
      final beginTime = '${_selectedDate.year}-${_selectedDate.month.toString().padLeft(2, '0')}-${_selectedDate.day.toString().padLeft(2, '0')} 00:00:00';
      result = await api.placeCycleOrder(
        mobile: _mobileController.text,
        comboId: _selectedComboId!,
        serviceInfoId: _selectedAddressId!,
        sellerId: _selectedSellerId!,
        weekType: _weekType,
        serverTimeCycles: '1~${_selectedTime.hour}:${_selectedTime.minute}~${_selectedTime.hour + 4}:${_selectedTime.minute}',
        beginServerTime: beginTime,
      );
    } else {
      // 单次单
      result = await api.placeSingleOrder(
        mobile: _mobileController.text,
        comboId: _selectedComboId!,
        serviceInfoId: _selectedAddressId!,
        sellerId: _selectedSellerId!,
        serviceTime: serviceTime,
      );
    }

    setState(() => _loading = false);

    if (result['code'] == 0) {
      Get.snackbar('成功', '下单成功，订单号: ${result['data']?['order_id']}');
    } else {
      Get.snackbar('失败', result['message'] ?? '下单失败');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('手动下单'),
        centerTitle: true,
      ),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // 手机号
                  TextField(
                    controller: _mobileController,
                    decoration: const InputDecoration(
                      labelText: '客户手机号',
                      prefixIcon: Icon(Icons.phone),
                      border: OutlineInputBorder(),
                    ),
                    keyboardType: TextInputType.phone,
                    onSubmitted: (_) => _queryCombos(),
                  ),
                  const SizedBox(height: 8),
                  ElevatedButton(
                    onPressed: _queryCombos,
                    child: const Text('查询套餐'),
                  ),
                  const SizedBox(height: 16),

                  // 套餐选择
                  if (_combos.isNotEmpty) ...[
                    DropdownButtonFormField<String>(
                      value: _selectedComboId,
                      decoration: const InputDecoration(
                        labelText: '选择套餐',
                        border: OutlineInputBorder(),
                      ),
                      items: _combos.map<DropdownMenuItem<String>>((c) {
                        return DropdownMenuItem(
                          value: c['comboOrderId']?.toString(),
                          child: Text(c['orderListTitle'] ?? ''),
                        );
                      }).toList(),
                      onChanged: (v) {
                        setState(() {
                          _selectedComboId = v;
                          _loadAddresses();
                        });
                      },
                    ),
                    const SizedBox(height: 16),

                    // 地址选择
                    if (_addresses.isNotEmpty)
                      DropdownButtonFormField<String>(
                        value: _selectedAddressId,
                        decoration: const InputDecoration(
                          labelText: '服务地址',
                          border: OutlineInputBorder(),
                        ),
                        items: _addresses.map<DropdownMenuItem<String>>((a) {
                          return DropdownMenuItem(
                            value: a['id']?.toString(),
                            child: Text(a['address'] ?? ''),
                          );
                        }).toList(),
                        onChanged: (v) => setState(() => _selectedAddressId = v),
                      ),

                    // 新地址输入
                    TextField(
                      controller: _addressController,
                      decoration: const InputDecoration(
                        labelText: '或输入新地址',
                        border: OutlineInputBorder(),
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // 分配方式
                  Row(
                    children: [
                      const Text('分配方式: '),
                      Radio<String>(
                        value: 'random',
                        groupValue: _assignMode,
                        onChanged: (v) => setState(() => _assignMode = v!),
                      ),
                      const Text('随机'),
                      Radio<String>(
                        value: 'assigned',
                        groupValue: _assignMode,
                        onChanged: (v) => setState(() => _assignMode = v!),
                      ),
                      const Text('指定'),
                    ],
                  ),

                  // 指定保洁师
                  if (_assignMode == 'assigned') ...[
                    const SizedBox(height: 8),
                    TextField(
                      controller: _sellerMobileController,
                      decoration: const InputDecoration(
                        labelText: '保洁师手机号',
                        border: OutlineInputBorder(),
                      ),
                      keyboardType: TextInputType.phone,
                    ),
                    const SizedBox(height: 8),
                    ElevatedButton(
                      onPressed: () async {
                        if (_sellerMobileController.text.isEmpty) return;
                        final api = Get.find<ApiService>();
                        final sellerId = await api.getSellerByMobile(_sellerMobileController.text);
                        if (sellerId != null) {
                          setState(() => _selectedSellerId = sellerId);
                          Get.snackbar('成功', '找到保洁师ID: $sellerId');
                        } else {
                          Get.snackbar('失败', '未找到该保洁师');
                        }
                      },
                      child: const Text('查询保洁师'),
                    ),
                  ] else ...[
                    ElevatedButton(
                      onPressed: _searchSellers,
                      child: const Text('搜索附近保洁师'),
                    ),
                  ],

                  // 保洁师选择
                  if (_sellers.isNotEmpty) ...[
                    const SizedBox(height: 16),
                    DropdownButtonFormField<String>(
                      value: _selectedSellerId,
                      decoration: const InputDecoration(
                        labelText: '选择保洁师',
                        border: OutlineInputBorder(),
                      ),
                      items: _sellers.map<DropdownMenuItem<String>>((s) {
                        return DropdownMenuItem(
                          value: s['seller_id'],
                          child: Text('${s['seller_name']} (${s['distance']}m)'),
                        );
                      }).toList(),
                      onChanged: (v) => setState(() => _selectedSellerId = v),
                    ),
                  ],

                  const SizedBox(height: 16),

                  // 服务时间
                  Row(
                    children: [
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            final date = await showDatePicker(
                              context: context,
                              initialDate: _selectedDate,
                              firstDate: DateTime.now(),
                              lastDate: DateTime.now().add(const Duration(days: 14)),
                            );
                            if (date != null) {
                              setState(() => _selectedDate = date);
                            }
                          },
                          icon: const Icon(Icons.calendar_today),
                          label: Text('${_selectedDate.year}-${_selectedDate.month}-${_selectedDate.day}'),
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: OutlinedButton.icon(
                          onPressed: () async {
                            final time = await showTimePicker(
                              context: context,
                              initialTime: _selectedTime,
                            );
                            if (time != null) {
                              setState(() => _selectedTime = time);
                            }
                          },
                          icon: const Icon(Icons.access_time),
                          label: Text('${_selectedTime.hour}:${_selectedTime.minute}'),
                        ),
                      ),
                    ],
                  ),

                  const SizedBox(height: 16),

                  // 订单类型
                  SwitchListTile(
                    title: const Text('周期订单'),
                    subtitle: const Text('开启后将创建周期保洁服务'),
                    value: _isCycle,
                    onChanged: (v) => setState(() => _isCycle = v),
                  ),

                  if (_isCycle) ...[
                    const Text('频次类型:'),
                    Row(
                      children: [
                        Radio<int>(
                          value: 1,
                          groupValue: _weekType,
                          onChanged: (v) => setState(() => _weekType = v!),
                        ),
                        const Text('一周一次'),
                        Radio<int>(
                          value: 2,
                          groupValue: _weekType,
                          onChanged: (v) => setState(() => _weekType = v!),
                        ),
                        const Text('一周多次'),
                        Radio<int>(
                          value: 3,
                          groupValue: _weekType,
                          onChanged: (v) => setState(() => _weekType = v!),
                        ),
                        const Text('二周一次'),
                      ],
                    ),
                  ],

                  const SizedBox(height: 24),

                  // 下单按钮
                  FilledButton.icon(
                    onPressed: _placeOrder,
                    icon: const Icon(Icons.check),
                    label: const Text('确认下单'),
                    style: FilledButton.styleFrom(
                      padding: const EdgeInsets.all(16),
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
