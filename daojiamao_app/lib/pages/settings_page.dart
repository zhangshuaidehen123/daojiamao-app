import 'package:flutter/material.dart';
import 'package:get/get.dart';
import '../services/api_service.dart';

class SettingsPage extends StatefulWidget {
  const SettingsPage({super.key});

  @override
  State<SettingsPage> createState() => _SettingsPageState();
}

class _SettingsPageState extends State<SettingsPage> {
  final _serverUrlController = TextEditingController();
  final _cookieController = TextEditingController();
  bool _showCookie = false;

  @override
  void initState() {
    super.initState();
    final api = Get.find<ApiService>();
    _serverUrlController.text = api.baseUrl;
    _loadCookieStatus();
  }

  @override
  void dispose() {
    _serverUrlController.dispose();
    _cookieController.dispose();
    super.dispose();
  }

  Future<void> _loadCookieStatus() async {
    final api = Get.find<ApiService>();
    final status = await api.getCookieStatus();
    if (status['code'] == 0) {
      // 获取当前Cookie内容
      try {
        final res = await api._dio.get('/api/cookie/download');
        if (res.data['code'] == 0) {
          _cookieController.text = res.data['data']?['content'] ?? '';
        }
      } catch (e) {
        // ignore
      }
    }
  }

  void _saveServerUrl() {
    final url = _serverUrlController.text.trim();
    if (url.isEmpty) {
      Get.snackbar('提示', '请输入服务器地址');
      return;
    }

    final api = Get.find<ApiService>();
    api.updateBaseUrl(url);
    Get.snackbar('成功', '服务器地址已更新');
  }

  Future<void> _saveCookie() async {
    final content = _cookieController.text.trim();
    if (content.isEmpty) {
      Get.snackbar('提示', '请输入Cookie内容');
      return;
    }

    final api = Get.find<ApiService>();
    final success = await api.updateCookie(content);

    if (success) {
      Get.snackbar('成功', 'Cookie已更新');
    } else {
      Get.snackbar('失败', 'Cookie更新失败');
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('设置'),
        centerTitle: true,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // 服务器配置
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.dns, color: Colors.blue),
                      const SizedBox(width: 8),
                      Text(
                        '服务器配置',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _serverUrlController,
                    decoration: const InputDecoration(
                      labelText: 'API服务器地址',
                      hintText: 'http://192.168.1.100:5000',
                      border: OutlineInputBorder(),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '说明：手机和电脑需在同一局域网',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: ElevatedButton(
                      onPressed: _saveServerUrl,
                      child: const Text('保存'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // Cookie管理
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.cookie, color: Colors.orange),
                      const SizedBox(width: 8),
                      Text(
                        'Cookie管理',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: _cookieController,
                    maxLines: _showCookie ? 10 : 3,
                    decoration: InputDecoration(
                      labelText: 'Cookie内容',
                      hintText: '粘贴Cookie内容...',
                      border: const OutlineInputBorder(),
                      suffixIcon: IconButton(
                        icon: Icon(_showCookie ? Icons.visibility_off : Icons.visibility),
                        onPressed: () => setState(() => _showCookie = !_showCookie),
                      ),
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    '提示：从浏览器Cookie编辑器导出并粘贴到此处',
                    style: Theme.of(context).textTheme.bodySmall,
                  ),
                  const SizedBox(height: 8),
                  Align(
                    alignment: Alignment.centerRight,
                    child: ElevatedButton.icon(
                      onPressed: _saveCookie,
                      icon: const Icon(Icons.save),
                      label: const Text('保存Cookie'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 使用说明
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.help_outline, color: Colors.green),
                      const SizedBox(width: 8),
                      Text(
                        '使用说明',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  _buildHelpItem('1', '在PC端运行Python API服务'),
                  _buildHelpItem('2', '确保手机和PC在同一局域网'),
                  _buildHelpItem('3', '配置正确的服务器地址'),
                  _buildHelpItem('4', '更新Cookie（从浏览器导出）'),
                  _buildHelpItem('5', '开始使用手动下单功能'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),

          // 关于
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.info_outline, color: Colors.grey),
                      const SizedBox(width: 8),
                      Text(
                        '关于',
                        style: Theme.of(context).textTheme.titleMedium,
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  const ListTile(
                    title: Text('到家保洁手动下单App'),
                    subtitle: Text('版本 1.0.0'),
                  ),
                  const Divider(),
                  const ListTile(
                    title: Text('基于原有自动化系统重构'),
                    subtitle: Text('保留核心下单逻辑，支持手机端手动操作'),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildHelpItem(String number, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          CircleAvatar(
            radius: 12,
            backgroundColor: Colors.blue,
            child: Text(
              number,
              style: const TextStyle(color: Colors.white, fontSize: 12),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(child: Text(text)),
        ],
      ),
    );
  }
}
