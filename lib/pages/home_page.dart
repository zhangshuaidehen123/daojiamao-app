import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../services/api_service.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    final api = Get.find<ApiService>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('到家保洁'),
        centerTitle: true,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              // 刷新Cookie状态
            },
          ),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: () async {},
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            // Cookie状态卡片
            _buildCookieStatusCard(api),
            const SizedBox(height: 16),

            // 快捷操作
            _buildQuickActionsCard(context),
            const SizedBox(height: 16),

            // 今日统计
            _buildTodayStatsCard(),
          ],
        ),
      ),
    );
  }

  Widget _buildCookieStatusCard(ApiService api) {
    return FutureBuilder<Map>(
      future: api.getCookieStatus(),
      builder: (context, snapshot) {
        final exists = snapshot.data?['data']?['exists'] == true;
        final valid = snapshot.data?['data']?['valid'] == true;

        return Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Row(
              children: [
                Icon(
                  exists ? Icons.check_circle : Icons.error,
                  color: exists ? Colors.green : Colors.red,
                  size: 48,
                ),
                const SizedBox(width: 16),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Cookie状态',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                      const SizedBox(height: 4),
                      Text(
                        exists ? '已配置' : '未配置',
                        style: TextStyle(
                          color: exists ? Colors.green : Colors.red,
                        ),
                      ),
                    ],
                  ),
                ),
                TextButton(
                  onPressed: () {},
                  child: const Text('管理'),
                ),
              ],
            ),
          ),
        );
      },
    );
  }

  Widget _buildQuickActionsCard(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '快捷操作',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildQuickAction(
                  icon: Icons.add_shopping_cart,
                  label: '下单',
                  color: Colors.blue,
                  onTap: () {},
                ),
                _buildQuickAction(
                  icon: Icons.history,
                  label: '订单查询',
                  color: Colors.orange,
                  onTap: () {},
                ),
                _buildQuickAction(
                  icon: Icons.monetization_on,
                  label: '现金结算',
                  color: Colors.green,
                  onTap: () {},
                ),
                _buildQuickAction(
                  icon: Icons.person_search,
                  label: '查保洁师',
                  color: Colors.purple,
                  onTap: () {},
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildQuickAction({
    required IconData icon,
    required String label,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(8),
      child: Padding(
        padding: const EdgeInsets.all(8),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: color.withOpacity(0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(icon, color: color, size: 28),
            ),
            const SizedBox(height: 8),
            Text(label, style: const TextStyle(fontSize: 12)),
          ],
        ),
      ),
    );
  }

  Widget _buildTodayStatsCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              '今日统计',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceAround,
              children: [
                _buildStatItem('已处理', '0', Colors.blue),
                _buildStatItem('成功', '0', Colors.green),
                _buildStatItem('失败', '0', Colors.red),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatItem(String label, String value, Color color) {
    return Column(
      children: [
        Text(
          value,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.bold,
            color: color,
          ),
        ),
        Text(label, style: const TextStyle(color: Colors.grey)),
      ],
    );
  }
}
