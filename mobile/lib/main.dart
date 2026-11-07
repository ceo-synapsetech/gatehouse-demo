import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

const _apiBase = String.fromEnvironment(
  'GATEHOUSE_API_BASE',
  defaultValue: 'https://gatehouse-demo.onrender.com',
);

const _forest = Color(0xFF1B5A46);
const _blue = Color(0xFF1D627C);
const _paper = Color(0xFFF4F7F4);
const _ink = Color(0xFF182824);
const _muted = Color(0xFF71807B);

void main() => runApp(const GatehouseApp());

enum UserRole { security, resident }

extension UserRoleStyle on UserRole {
  String get label => this == UserRole.security ? 'Security' : 'Resident';
  Color get color => this == UserRole.security ? _forest : _blue;
  String get description => this == UserRole.security
      ? 'Manage access requests at the front desk.'
      : 'Review requests sent to your home.';
}

class GatehouseApp extends StatelessWidget {
  const GatehouseApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Gatehouse',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(seedColor: _forest),
      scaffoldBackgroundColor: _paper,
      appBarTheme: const AppBarTheme(
        backgroundColor: Colors.white,
        foregroundColor: _ink,
        surfaceTintColor: Colors.white,
      ),
      cardTheme: CardThemeData(
        color: Colors.white,
        elevation: 0,
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(18),
          side: const BorderSide(color: Color(0xFFE4EAE6)),
        ),
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: Colors.white,
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFE4EAE6)),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(12),
          borderSide: const BorderSide(color: Color(0xFFE4EAE6)),
        ),
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 14,
          vertical: 14,
        ),
      ),
    ),
    home: const GatehouseHome(),
  );
}

class ApiException implements Exception {
  const ApiException(this.message);
  final String message;
  @override
  String toString() => message;
}

class ApiClient {
  ApiClient({required this.username, required this.password});

  final String username;
  final String password;
  final HttpClient _client = HttpClient()
    ..connectionTimeout = const Duration(seconds: 15);

  Future<dynamic> _request(
    String path, {
    String method = 'GET',
    Map<String, Object?>? body,
  }) async {
    try {
      final request = await _client.openUrl(
        method,
        Uri.parse('$_apiBase$path'),
      );
      final basic = base64Encode(utf8.encode('$username:$password'));
      request.headers
        ..set(HttpHeaders.authorizationHeader, 'Basic $basic')
        ..set(HttpHeaders.acceptHeader, 'application/json');
      if (body != null) {
        request.headers.contentType = ContentType.json;
        request.write(jsonEncode(body));
      }
      final response = await request.close();
      final text = await response.transform(utf8.decoder).join();
      if (response.statusCode < 200 || response.statusCode >= 300) {
        throw ApiException(
          response.statusCode == 401
              ? 'Those credentials were not accepted.'
              : 'The server returned ${response.statusCode}. ${text.trim()}'
                    .trim(),
        );
      }
      return text.isEmpty ? null : jsonDecode(text);
    } on SocketException {
      throw const ApiException(
        'Could not connect. Check your internet connection and try again.',
      );
    } on HandshakeException {
      throw const ApiException(
        'Could not establish a secure connection to Gatehouse.',
      );
    } on FormatException {
      throw const ApiException('The server returned an unexpected response.');
    }
  }

  Future<Profile> signIn() async =>
      Profile.fromJson(await _request('/api/auth/me') as Map<String, dynamic>);

  Future<List<AccessRequest>> requests() async {
    final result = await _request('/api/requests') as List<dynamic>;
    return result
        .map((item) => AccessRequest.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> createRequest({
    required String unit,
    required String kind,
    required String person,
    required String details,
  }) async {
    await _request(
      '/api/requests',
      method: 'POST',
      body: {'unit': unit, 'kind': kind, 'person': person, 'details': details},
    );
  }

  Future<void> decide(int id, String status) async {
    await _request(
      '/api/requests/$id/decision',
      method: 'PATCH',
      body: {'status': status},
    );
  }

  Future<List<AppNotification>> notifications() async {
    final result = await _request('/api/notifications') as List<dynamic>;
    return result
        .map((item) => AppNotification.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  Future<void> markNotificationRead(int id) async {
    await _request('/api/notifications/$id/read', method: 'PATCH');
  }

  void close() => _client.close(force: true);
}

class Profile {
  const Profile({required this.username, required this.role, this.unit});
  final String username;
  final UserRole role;
  final String? unit;

  factory Profile.fromJson(Map<String, dynamic> json) => Profile(
    username: json['username'] as String? ?? '',
    role: json['role'] == 'RESIDENT' ? UserRole.resident : UserRole.security,
    unit: json['unit'] as String?,
  );
}

class AccessRequest {
  const AccessRequest({
    required this.id,
    required this.unit,
    required this.residentName,
    required this.kind,
    required this.person,
    required this.details,
    required this.status,
    required this.createdAt,
  });

  final int id;
  final String unit;
  final String residentName;
  final String kind;
  final String person;
  final String details;
  final String status;
  final DateTime? createdAt;

  factory AccessRequest.fromJson(Map<String, dynamic> json) => AccessRequest(
    id: (json['id'] as num).toInt(),
    unit: json['unit'] as String? ?? '',
    residentName: json['residentName'] as String? ?? 'Resident',
    kind: json['kind'] as String? ?? 'Access request',
    person: json['person'] as String? ?? '',
    details: json['details'] as String? ?? '',
    status: json['status'] as String? ?? 'PENDING',
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
  );
}

class AppNotification {
  const AppNotification({
    required this.id,
    required this.requestId,
    required this.title,
    required this.message,
    required this.createdAt,
    required this.readAt,
  });

  final int id;
  final int? requestId;
  final String title;
  final String message;
  final DateTime? createdAt;
  final DateTime? readAt;
  bool get isRead => readAt != null;

  factory AppNotification.fromJson(Map<String, dynamic> json) => AppNotification(
    id: (json['id'] as num).toInt(),
    requestId: (json['requestId'] as num?)?.toInt(),
    title: json['title'] as String? ?? 'Gatehouse notification',
    message: json['message'] as String? ?? '',
    createdAt: DateTime.tryParse(json['createdAt'] as String? ?? ''),
    readAt: DateTime.tryParse(json['readAt'] as String? ?? ''),
  );
}

class GatehouseHome extends StatefulWidget {
  const GatehouseHome({super.key});
  @override
  State<GatehouseHome> createState() => _GatehouseHomeState();
}

class _GatehouseHomeState extends State<GatehouseHome> {
  ApiClient? _api;
  Profile? _profile;

  Future<void> _signIn(
    String username,
    String password,
    UserRole selectedRole,
  ) async {
    final api = ApiClient(username: username.trim(), password: password);
    try {
      final profile = await api.signIn();
      if (profile.role != selectedRole) {
        api.close();
        throw const ApiException(
          'Choose the sign-in type that matches this account.',
        );
      }
      if (!mounted) {
        api.close();
        return;
      }
      setState(() {
        _api = api;
        _profile = profile;
      });
    } on ApiException {
      api.close();
      rethrow;
    } catch (_) {
      api.close();
      throw const ApiException('Sign-in failed. Please try again.');
    }
  }

  void _signOut() {
    _api?.close();
    setState(() {
      _api = null;
      _profile = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    final api = _api;
    final profile = _profile;
    if (api != null && profile != null) {
      return DashboardScreen(api: api, profile: profile, onSignOut: _signOut);
    }
    return SignInScreen(onSignIn: _signIn);
  }
}

class SignInScreen extends StatefulWidget {
  const SignInScreen({required this.onSignIn, super.key});
  final Future<void> Function(String, String, UserRole) onSignIn;
  @override
  State<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends State<SignInScreen> {
  final _formKey = GlobalKey<FormState>();
  final _username = TextEditingController(text: 'guard01');
  final _password = TextEditingController();
  UserRole _role = UserRole.security;
  bool _loading = false;
  bool _showPassword = false;
  String? _error;

  @override
  void dispose() {
    _username.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      await widget.onSignIn(_username.text, _password.text, _role);
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final color = _role.color;
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(22),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const SizedBox(height: 22),
                  Icon(Icons.home_work_rounded, color: color, size: 42),
                  const SizedBox(height: 12),
                  const Text(
                    'Gatehouse',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                      color: _ink,
                    ),
                  ),
                  const SizedBox(height: 6),
                  const Text(
                    'A clear front door for your community.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _muted),
                  ),
                  const SizedBox(height: 28),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Text(
                            'Sign in',
                            style: Theme.of(context).textTheme.headlineSmall
                                ?.copyWith(fontWeight: FontWeight.w800),
                          ),
                          const SizedBox(height: 5),
                          Text(
                            _role.description,
                            style: const TextStyle(color: _muted),
                          ),
                          const SizedBox(height: 20),
                          SegmentedButton<UserRole>(
                            segments: const [
                              ButtonSegment(
                                value: UserRole.security,
                                label: Text('Security'),
                                icon: Icon(Icons.shield_outlined),
                              ),
                              ButtonSegment(
                                value: UserRole.resident,
                                label: Text('Resident'),
                                icon: Icon(Icons.home_outlined),
                              ),
                            ],
                            selected: {_role},
                            onSelectionChanged: _loading
                                ? null
                                : (roles) {
                                    setState(() {
                                      _role = roles.first;
                                      _username.text =
                                          _role == UserRole.security
                                          ? 'guard01'
                                          : 'ananya@example.com';
                                      _error = null;
                                    });
                                  },
                          ),
                          const SizedBox(height: 20),
                          Form(
                            key: _formKey,
                            child: Column(
                              children: [
                                TextFormField(
                                  controller: _username,
                                  autocorrect: false,
                                  textInputAction: TextInputAction.next,
                                  decoration: InputDecoration(
                                    labelText: _role == UserRole.security
                                        ? 'Guard ID'
                                        : 'Resident email',
                                    prefixIcon: const Icon(
                                      Icons.person_outline,
                                    ),
                                  ),
                                  validator: (value) =>
                                      value == null || value.trim().isEmpty
                                      ? 'Enter your account ID.'
                                      : null,
                                ),
                                const SizedBox(height: 14),
                                TextFormField(
                                  controller: _password,
                                  obscureText: !_showPassword,
                                  textInputAction: TextInputAction.done,
                                  onFieldSubmitted: (_) => _submit(),
                                  decoration: InputDecoration(
                                    labelText: 'Password',
                                    prefixIcon: const Icon(Icons.lock_outline),
                                    suffixIcon: IconButton(
                                      tooltip: _showPassword
                                          ? 'Hide password'
                                          : 'Show password',
                                      onPressed: () => setState(
                                        () => _showPassword = !_showPassword,
                                      ),
                                      icon: Icon(
                                        _showPassword
                                            ? Icons.visibility_off_outlined
                                            : Icons.visibility_outlined,
                                      ),
                                    ),
                                  ),
                                  validator: (value) =>
                                      value == null || value.isEmpty
                                      ? 'Enter your password.'
                                      : null,
                                ),
                              ],
                            ),
                          ),
                          if (_error != null) ...[
                            const SizedBox(height: 14),
                            Text(
                              _error!,
                              style: const TextStyle(color: Color(0xFF9D483C)),
                            ),
                          ],
                          const SizedBox(height: 18),
                          FilledButton.icon(
                            onPressed: _loading ? null : _submit,
                            style: FilledButton.styleFrom(
                              backgroundColor: color,
                              padding: const EdgeInsets.symmetric(vertical: 14),
                            ),
                            icon: _loading
                                ? const SizedBox(
                                    width: 18,
                                    height: 18,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Icon(Icons.arrow_forward_rounded),
                            label: Text(_loading ? 'Signing in…' : 'Continue'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Demo accounts: guard01 / demo123 · ananya@example.com / demo123',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: _muted, fontSize: 11),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class DashboardScreen extends StatefulWidget {
  const DashboardScreen({
    required this.api,
    required this.profile,
    required this.onSignOut,
    super.key,
  });
  final ApiClient api;
  final Profile profile;
  final VoidCallback onSignOut;
  @override
  State<DashboardScreen> createState() => _DashboardScreenState();
}

class _DashboardScreenState extends State<DashboardScreen> {
  List<AccessRequest> _requests = [];
  bool _loading = true;
  bool _working = false;
  bool _compact = false;
  bool _showTimestamps = true;
  List<AppNotification> _notifications = [];
  Timer? _notificationTimer;
  String? _error;

  UserRole get _role => widget.profile.role;
  Color get _accent => _role.color;
  List<AccessRequest> get _pending =>
      _requests.where((item) => item.status == 'PENDING').toList();
  List<AccessRequest> get _history =>
      _requests.where((item) => item.status != 'PENDING').toList();

  @override
  void initState() {
    super.initState();
    _loadRequests();
    if (_role == UserRole.resident) {
      _loadNotifications();
      _notificationTimer = Timer.periodic(
        const Duration(seconds: 20),
        (_) => _loadNotifications(silent: true),
      );
    }
  }

  @override
  void dispose() {
    _notificationTimer?.cancel();
    super.dispose();
  }

  Future<void> _loadNotifications({bool silent = false}) async {
    try {
      final items = await widget.api.notifications();
      if (mounted) setState(() => _notifications = items);
    } on ApiException catch (error) {
      if (!silent && mounted) _notice(error.message);
    }
  }

  Future<void> _openNotifications() async {
    await _loadNotifications();
    if (!mounted) return;
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      showDragHandle: true,
      builder: (sheetContext) => SafeArea(
        child: SizedBox(
          height: MediaQuery.sizeOf(sheetContext).height * .72,
          child: Column(
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(20, 4, 20, 12),
                child: Row(
                  children: [
                    Expanded(
                      child: Text('Notifications', style: Theme.of(sheetContext).textTheme.titleLarge?.copyWith(fontWeight: FontWeight.w800)),
                    ),
                    TextButton(onPressed: () => _loadNotifications(), child: const Text('Refresh')),
                  ],
                ),
              ),
              Expanded(
                child: _notifications.isEmpty
                    ? const Center(child: Text('You’re all caught up.', style: TextStyle(color: _muted)))
                    : ListView.separated(
                        itemCount: _notifications.length,
                        separatorBuilder: (context, index) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final item = _notifications[index];
                          return ListTile(
                            leading: Icon(item.isRead ? Icons.notifications_none : Icons.notifications_active_outlined, color: item.isRead ? _muted : _blue),
                            title: Text(item.title, style: TextStyle(fontWeight: item.isRead ? FontWeight.w500 : FontWeight.w800)),
                            subtitle: Text(item.message),
                            trailing: item.isRead ? null : const Icon(Icons.circle, size: 9, color: _blue),
                            onTap: () async {
                              if (!item.isRead) {
                                try {
                                  await widget.api.markNotificationRead(item.id);
                                  await _loadNotifications(silent: true);
                                } on ApiException catch (error) {
                                  if (mounted) _notice(error.message);
                                }
                              }
                              if (sheetContext.mounted) Navigator.pop(sheetContext);
                              await _loadRequests();
                            },
                          );
                        },
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> _loadRequests() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final items = await widget.api.requests();
      if (mounted) setState(() => _requests = items);
    } on ApiException catch (error) {
      if (mounted) setState(() => _error = error.message);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _decide(AccessRequest request, String status) async {
    setState(() => _working = true);
    try {
      await widget.api.decide(request.id, status);
      await _loadRequests();
      if (mounted)
        _notice(
          status == 'APPROVED' ? 'Request approved.' : 'Request declined.',
        );
    } on ApiException catch (error) {
      if (mounted) _notice(error.message);
    } finally {
      if (mounted) setState(() => _working = false);
    }
  }

  void _notice(String message) => ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text(message), behavior: SnackBarBehavior.floating),
  );

  Widget _profileLine(String label, String value) => Padding(
    padding: const EdgeInsets.only(top: 10),
    child: Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(label, style: const TextStyle(fontSize: 12, color: _muted)),
        const SizedBox(height: 2),
        Text(value, style: const TextStyle(fontWeight: FontWeight.w700)),
      ],
    ),
  );

  Future<void> _showProfile() => showDialog<void>(
    context: context,
    builder: (context) => AlertDialog(
      title: const Text('Profile'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _profileLine('Account', widget.profile.username),
          _profileLine('Role', _role.label),
          if (widget.profile.unit != null)
            _profileLine('Home unit', widget.profile.unit!),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context),
          child: const Text('Done'),
        ),
      ],
    ),
  );

  Future<void> _showSettings() => showModalBottomSheet<void>(
    context: context,
    showDragHandle: true,
    builder: (context) => StatefulBuilder(
      builder: (context, setSheetState) => SafeArea(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(20, 0, 20, 22),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: Text(
                  'Settings',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Show timestamps'),
                subtitle: const Text('Display when each request was created.'),
                value: _showTimestamps,
                onChanged: (value) {
                  setState(() => _showTimestamps = value);
                  setSheetState(() {});
                },
              ),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text('Compact request cards'),
                subtitle: const Text('Reduce spacing in the request list.'),
                value: _compact,
                onChanged: (value) {
                  setState(() => _compact = value);
                  setSheetState(() {});
                },
              ),
            ],
          ),
        ),
      ),
    ),
  );

  Future<void> _newRequest() async {
    final result = await showDialog<_NewRequestData>(
      context: context,
      builder: (_) => const _NewRequestDialog(),
    );
    if (result == null) return;
    setState(() => _working = true);
    try {
      await widget.api.createRequest(
        unit: result.unit,
        kind: result.kind,
        person: result.person,
        details: result.details,
      );
      await _loadRequests();
      if (mounted) _notice('Request sent to the resident.');
    } on ApiException catch (error) {
      if (mounted) _notice(error.message);
    } finally {
      if (mounted) setState(() => _working = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final userName = widget.profile.username.split('@').first;
    final name = userName.isEmpty
        ? 'Resident'
        : '${userName[0].toUpperCase()}${userName.substring(1)}';
    final greeting = _role == UserRole.resident
        ? 'Welcome home, $name'
        : 'Security desk';
    return Scaffold(
      appBar: AppBar(
        title: Row(
          children: [
            Icon(Icons.home_work_rounded, color: _accent),
            const SizedBox(width: 8),
            const Text(
              'Gatehouse',
              style: TextStyle(fontWeight: FontWeight.w800),
            ),
          ],
        ),
        actions: [
          if (_role == UserRole.resident)
            IconButton(
              tooltip: 'Notifications',
              onPressed: _openNotifications,
              icon: Stack(
                clipBehavior: Clip.none,
                children: [
                  const Icon(Icons.notifications_outlined),
                  if (_notifications.any((item) => !item.isRead))
                    Positioned(
                      right: -3,
                      top: -3,
                      child: Container(
                        width: 10,
                        height: 10,
                        decoration: const BoxDecoration(color: Color(0xFFC94F43), shape: BoxShape.circle),
                      ),
                    ),
                ],
              ),
            ),
          PopupMenuButton<String>(
            tooltip: 'Account menu',
            icon: const Icon(Icons.menu_rounded),
            onSelected: (value) {
              if (value == 'profile') _showProfile();
              if (value == 'settings') _showSettings();
              if (value == 'logout') widget.onSignOut();
            },
            itemBuilder: (context) => const [
              PopupMenuItem(
                value: 'profile',
                child: ListTile(
                  leading: Icon(Icons.person_outline),
                  title: Text('Profile'),
                  contentPadding: EdgeInsets.zero,
                ),
              ),
              PopupMenuItem(
                value: 'settings',
                child: ListTile(
                  leading: Icon(Icons.tune_rounded),
                  title: Text('Settings'),
                  contentPadding: EdgeInsets.zero,
                ),
              ),
              PopupMenuItem(
                value: 'logout',
                child: ListTile(
                  leading: Icon(Icons.logout_rounded),
                  title: Text('Log out'),
                  contentPadding: EdgeInsets.zero,
                ),
              ),
            ],
          ),
          const SizedBox(width: 6),
        ],
      ),
      body: RefreshIndicator(
        onRefresh: _loadRequests,
        child: ListView(
          padding: const EdgeInsets.fromLTRB(18, 18, 18, 32),
          children: [
            Text(
              _role == UserRole.resident
                  ? 'RESIDENT PORTAL'
                  : 'SECURITY PORTAL · FRONT DESK',
              style: TextStyle(
                color: _accent,
                fontWeight: FontWeight.w800,
                fontSize: 11,
                letterSpacing: 1.1,
              ),
            ),
            const SizedBox(height: 6),
            Text(
              greeting,
              style: const TextStyle(
                fontSize: 26,
                fontWeight: FontWeight.w800,
                color: _ink,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              _role == UserRole.resident
                  ? 'Review access requests sent to ${widget.profile.unit ?? 'your home'}.'
                  : 'Create requests and track resident decisions.',
              style: const TextStyle(color: _muted),
            ),
            const SizedBox(height: 18),
            _statsCard(),
            const SizedBox(height: 14),
            if (_role == UserRole.security) ...[
              FilledButton.icon(
                onPressed: _working ? null : _newRequest,
                icon: const Icon(Icons.add_rounded),
                label: const Text('New access request'),
                style: FilledButton.styleFrom(
                  backgroundColor: _accent,
                  padding: const EdgeInsets.symmetric(vertical: 14),
                ),
              ),
              const SizedBox(height: 18),
              _sectionTitle(
                'Request queue',
                subtitle: 'All units · newest first',
              ),
              const SizedBox(height: 8),
              _requestList(
                _requests,
                emptyTitle: 'No access requests yet',
                emptyDetail: 'New requests will appear here.',
              ),
            ] else ...[
              _sectionTitle(
                'Needs your attention',
                subtitle: 'Check the details before deciding.',
              ),
              const SizedBox(height: 8),
              _requestList(
                _pending,
                showActions: true,
                emptyTitle: 'You’re all caught up',
                emptyDetail: 'No requests are waiting for your decision.',
              ),
              const SizedBox(height: 22),
              _sectionTitle(
                'Decision history',
                subtitle: 'Your completed requests',
              ),
              const SizedBox(height: 8),
              _requestList(
                _history,
                emptyTitle: 'No decisions yet',
                emptyDetail: 'Completed requests will appear here.',
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _statsCard() {
    final approved = _requests
        .where((item) => item.status == 'APPROVED')
        .length;
    final denied = _requests.where((item) => item.status == 'DENIED').length;
    return Card(
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
        child: Row(
          children: [
            _stat('Waiting', _pending.length, const Color(0xFFA8741C)),
            _stat('Approved', approved, _forest),
            _stat('Declined', denied, const Color(0xFFA74C42)),
          ],
        ),
      ),
    );
  }

  Widget _stat(String label, int value, Color color) => Expanded(
    child: Column(
      children: [
        Text(
          '$value',
          style: TextStyle(
            color: color,
            fontSize: 25,
            fontWeight: FontWeight.w800,
          ),
        ),
        Text(
          label,
          style: const TextStyle(
            color: _muted,
            fontSize: 11,
            fontWeight: FontWeight.w600,
          ),
        ),
      ],
    ),
  );

  Widget _sectionTitle(String title, {required String subtitle}) => Row(
    children: [
      Expanded(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w800,
                color: _ink,
              ),
            ),
            const SizedBox(height: 3),
            Text(subtitle, style: const TextStyle(fontSize: 12, color: _muted)),
          ],
        ),
      ),
      IconButton(
        onPressed: _loadRequests,
        tooltip: 'Refresh',
        icon: const Icon(Icons.refresh_rounded),
      ),
    ],
  );

  Widget _requestList(
    List<AccessRequest> items, {
    bool showActions = false,
    required String emptyTitle,
    required String emptyDetail,
  }) {
    if (_loading && _requests.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(26),
          child: Center(child: CircularProgressIndicator()),
        ),
      );
    }
    if (_error != null && _requests.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(18),
          child: Column(
            children: [
              const Icon(Icons.cloud_off_outlined, color: _muted, size: 30),
              const SizedBox(height: 8),
              Text(
                _error!,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _muted),
              ),
              TextButton(
                onPressed: _loadRequests,
                child: const Text('Try again'),
              ),
            ],
          ),
        ),
      );
    }
    if (items.isEmpty) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Column(
            children: [
              Icon(
                _role == UserRole.resident
                    ? Icons.inbox_outlined
                    : Icons.fact_check_outlined,
                color: _accent,
                size: 28,
              ),
              const SizedBox(height: 8),
              Text(
                emptyTitle,
                style: const TextStyle(fontWeight: FontWeight.w700),
              ),
              const SizedBox(height: 4),
              Text(
                emptyDetail,
                textAlign: TextAlign.center,
                style: const TextStyle(color: _muted, fontSize: 12),
              ),
            ],
          ),
        ),
      );
    }
    return Column(
      children: items
          .map((item) => _requestCard(item, showActions: showActions))
          .toList(),
    );
  }

  Widget _requestCard(AccessRequest item, {required bool showActions}) {
    final statusColor = item.status == 'APPROVED'
        ? _forest
        : item.status == 'DENIED'
        ? const Color(0xFFA74C42)
        : const Color(0xFFA8741C);
    final date = item.createdAt == null
        ? null
        : MaterialLocalizations.of(context)
              .formatMediumDate(item.createdAt!.toLocal());
    return Card(
      margin: EdgeInsets.only(bottom: _compact ? 8 : 12),
      child: Padding(
        padding: EdgeInsets.all(_compact ? 14 : 16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        item.kind.toUpperCase(),
                        style: const TextStyle(
                          color: _muted,
                          fontSize: 10,
                          fontWeight: FontWeight.w800,
                          letterSpacing: .8,
                        ),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        item.person,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w800,
                          color: _ink,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 9,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: statusColor.withValues(alpha: .1),
                    borderRadius: BorderRadius.circular(30),
                  ),
                  child: Text(
                    item.status,
                    style: TextStyle(
                      color: statusColor,
                      fontWeight: FontWeight.w800,
                      fontSize: 10,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (_role == UserRole.security)
              Text(
                '${item.unit} · ${item.residentName}',
                style: const TextStyle(color: _muted, fontSize: 12),
              ),
            const SizedBox(height: 3),
            Text(
              item.details,
              style: const TextStyle(color: _ink, height: 1.4),
            ),
            if (_showTimestamps && date != null) ...[
              const SizedBox(height: 8),
              Text(date, style: const TextStyle(color: _muted, fontSize: 11)),
            ],
            if (showActions && item.status == 'PENDING') ...[
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: _working
                          ? null
                          : () => _decide(item, 'DENIED'),
                      icon: const Icon(Icons.close_rounded),
                      label: const Text('Decline'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: const Color(0xFFA74C42),
                      ),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: _working
                          ? null
                          : () => _decide(item, 'APPROVED'),
                      icon: const Icon(Icons.check_rounded),
                      label: const Text('Approve'),
                      style: FilledButton.styleFrom(backgroundColor: _blue),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _NewRequestData {
  const _NewRequestData(this.unit, this.kind, this.person, this.details);
  final String unit;
  final String kind;
  final String person;
  final String details;
}

class _NewRequestDialog extends StatefulWidget {
  const _NewRequestDialog();
  @override
  State<_NewRequestDialog> createState() => _NewRequestDialogState();
}

class _NewRequestDialogState extends State<_NewRequestDialog> {
  final _formKey = GlobalKey<FormState>();
  final _person = TextEditingController();
  final _details = TextEditingController();
  String _unit = 'A-204';
  String _kind = 'Delivery / visitor';

  @override
  void dispose() {
    _person.dispose();
    _details.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
    title: const Text('New access request'),
    content: SingleChildScrollView(
      child: Form(
        key: _formKey,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            DropdownButtonFormField<String>(
              initialValue: _kind,
              decoration: const InputDecoration(labelText: 'Request type'),
              items: const [
                DropdownMenuItem(
                  value: 'Delivery / visitor',
                  child: Text('Delivery / visitor'),
                ),
                DropdownMenuItem(
                  value: 'Taking item out',
                  child: Text('Taking item out'),
                ),
              ],
              onChanged: (value) => setState(() => _kind = value ?? _kind),
            ),
            const SizedBox(height: 12),
            DropdownButtonFormField<String>(
              initialValue: _unit,
              decoration: const InputDecoration(labelText: 'Resident / unit'),
              items: const [
                DropdownMenuItem(
                  value: 'A-204',
                  child: Text('Block A · Unit 204'),
                ),
                DropdownMenuItem(
                  value: 'B-101',
                  child: Text('Block B · Unit 101'),
                ),
                DropdownMenuItem(
                  value: 'C-302',
                  child: Text('Block C · Unit 302'),
                ),
              ],
              onChanged: (value) => setState(() => _unit = value ?? _unit),
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _person,
              decoration: const InputDecoration(labelText: 'Person / company'),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Enter a name.'
                  : null,
            ),
            const SizedBox(height: 12),
            TextFormField(
              controller: _details,
              minLines: 2,
              maxLines: 4,
              decoration: const InputDecoration(labelText: 'Details'),
              validator: (value) => value == null || value.trim().isEmpty
                  ? 'Add a short description.'
                  : null,
            ),
          ],
        ),
      ),
    ),
    actions: [
      TextButton(
        onPressed: () => Navigator.pop(context),
        child: const Text('Cancel'),
      ),
      FilledButton(
        onPressed: () {
          if (!_formKey.currentState!.validate()) return;
          Navigator.pop(
            context,
            _NewRequestData(
              _unit,
              _kind,
              _person.text.trim(),
              _details.text.trim(),
            ),
          );
        },
        child: const Text('Send request'),
      ),
    ],
  );
}

