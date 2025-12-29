import 'package:flutter/material.dart';
import 'package:get/get.dart';

class RegisterPage extends StatelessWidget {
  const RegisterPage({Key? key}) : super(key: key);

  Widget _buildTextField({
    required String hint,
    required IconData icon,
    bool obscure = false,
    Widget? suffix,
  }) {
    return TextField(
      obscureText: obscure,
      decoration: InputDecoration(
        prefixIcon: Icon(icon),
        hintText: hint,
        border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
        filled: true,
        fillColor: Colors.grey[100],
        suffixIcon: suffix,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('注册', style: TextStyle(fontWeight: FontWeight.bold)),
        centerTitle: true,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            const SizedBox(height: 24),
            const Text(
              '创建新账号',
              style: TextStyle(fontSize: 28, fontWeight: FontWeight.bold),
              textAlign: TextAlign.left,
            ),
            const SizedBox(height: 32),
            _buildTextField(hint: '邮箱', icon: Icons.email),
            const SizedBox(height: 16),
            _buildTextField(hint: '用户名', icon: Icons.person),
            const SizedBox(height: 16),
            _buildTextField(hint: '密码', icon: Icons.lock, obscure: true),
            const SizedBox(height: 16),
            _buildTextField(hint: '确认密码', icon: Icons.lock_outline, obscure: true),
            const SizedBox(height: 16),
            Row(
              children: [
                Expanded(
                  child: _buildTextField(hint: '验证码', icon: Icons.verified),
                ),
                const SizedBox(width: 12),
                SizedBox(
                  height: 48,
                  child: ElevatedButton(
                    onPressed: () {},
                    child: const Text('发送验证码'),
                    style: ElevatedButton.styleFrom(
                      minimumSize: const Size(100, 48),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 32),
            ElevatedButton(
              onPressed: () {},
              child: const Padding(
                padding: EdgeInsets.symmetric(vertical: 14),
                child: Text('注册', style: TextStyle(fontSize: 18)),
              ),
              style: ElevatedButton.styleFrom(
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}