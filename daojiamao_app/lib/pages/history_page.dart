import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../services/api_service.dart';

class HistoryPage extends StatefulWidget {
  const HistoryPage({super.key});

  @override
  State<HistoryPage> createState() => _HistoryPageState();
}

class _HistoryPageState extends State<HistoryPage> with SingleTickerProviderStateMixin {
  late TabController _tabController;
  final _mobileController = TextEditingController();

  List _orders = [];
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _mobileController.dispose();
    super.dispose();
  }

  Future<void> _queryOrders() async {
    if (_mobileController.text.isEmpty) {
      Get.snackbar('提示', '请输入手机号');
      return;
    }

    setState(() => _loading = true);
    final api = Get.find<ApiService>();
    final result = await api.queryOrders(mobile: _mobileController.text);
    setState(() {
      _loading = false;
      if (result['code'] == 0) {
        _orders = result['data']?['data']?['orderListDtoList'] ?? [];
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('订单查询'),
        centerTitle: true,
        bottom: TabBar(
          controller: _tabController,
          tabs: const [
            Tab(text: '订单列表'),
            Tab(text: '现金结算'),
          ],
        ),
      ),
      body: Column(
        children: [
          // 搜索栏
          Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _mobileController,
                    decoration: const InputDecoration(
                      hintText: '输入手机号查询',
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(horizontal: 16),
                    ),
                    keyboardType: TextInputType.phone,
                  ),
                ),
                const SizedBox(width: 8),
                ElevatedButton(
                  onPressed: _queryOrders,
                  child: const Text('查询'),
                ),
              ],
            ),
          ),

          // 订单列表
          Expanded(
            child: _loading
                ? const Center(child: CircularProgressIndicator())
                : TabBarView(
                    controller: _tabController,
                    children: [
                      _buildOrderList(),
                      _buildCashPayList(),
                    ],
                  ),
          ),
        ],
      ),
    );
  }

  Widget _buildOrderList() {
    if (_orders.isEmpty) {
      return const Center(
        child: Text('暂无订单，请先查询'),
      );
    }

    return ListView.builder(
      itemCount: _orders.length,
      itemBuilder: (context, index) {
        final order = _orders[index];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: ListTile(
            title: Text('订单号: ${order['orderId'] ?? ''}'),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('服务: ${order['serviceName'] ?? ''}'),
                Text('时间: ${order['serviceTime'] ?? ''}'),
                Text('地址: ${order['address'] ?? ''}'),
                Text('状态: ${order['orderStatusDesc'] ?? ''}'),
              ],
            ),
            isThreeLine: true,
            trailing: Text(
              '¥${order['payAmount'] ?? '0'}',
              style: const TextStyle(
                fontWeight: FontWeight.bold,
                fontSize: 16,
              ),
            ),
          ),
        );
      },
    );
  }

  Widget _buildCashPayList() {
    // 筛选待结算订单
    final pendingOrders = _orders.where((o) {
      return o['orderStatus'] == 2; // 待结算状态
    }).toList();

    if (pendingOrders.isEmpty) {
      return const Center(
        child: Text('暂无待结算订单'),
      );
    }

    return ListView.builder(
      itemCount: pendingOrders.length,
      itemBuilder: (context, index) {
        final order = pendingOrders[index];
        return Card(
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: ListTile(
            title: Text('订单号: ${order['orderId'] ?? ''}'),
            subtitle: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('服务: ${order['serviceName'] ?? ''}'),
                Text('金额: ¥${order['payAmount'] ?? '0'}'),
              ],
            ),
            trailing: ElevatedButton(
              onPressed: () => _showCashPayDialog(order),
              child: const Text('结算'),
            ),
          ),
        );
      },
    );
  }

  void _showCashPayDialog(Map order) {
    final amountController = TextEditingController(
      text: order['payAmount']?.toString() ?? '0',
    );

    Get.dialog(
      AlertDialog(
        title: const Text('现金结算'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('订单号: ${order['orderId']}'),
            const SizedBox(height: 16),
            TextField(
              controller: amountController,
              decoration: const InputDecoration(
                labelText: '收款金额',
                prefixText: '¥',
                border: OutlineInputBorder(),
              ),
              keyboardType: TextInputType.number,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Get.back(),
            child: const Text('取消'),
          ),
          ElevatedButton(
            onPressed: () async {
              final api = Get.find<ApiService>();
              final amount = double.tryParse(amountController.text) ?? 0;
              final success = await api.cashPay(
                order['orderId']?.toString() ?? '',
                amount,
              );

              Get.back();
              if (success) {
                Get.snackbar('成功', '结算成功');
                _queryOrders();
              } else {
                Get.snackbar('失败', '结算失败');
              }
            },
            child: const Text('确认'),
          ),
        ],
      ),
    );
  }
}
