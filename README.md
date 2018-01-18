# Cuckoo

通用数据全量采集

- 冷启动事件：应用第一次启动时的版本号、设备ID、渠道、启动时间、内存占用信息、磁盘占用信息。
- 前后台事件：应用进入前台或者后台。
- 页面事件：Activity/Fragment显示/隐藏事件。
- 控件点击事件：页面、弹窗上的控件点击事件。
- 列表浏览事件：被浏览的列表项、列表项的停留时间。
- 定位事件：用户的地理位置信息。
- 其他事件

- Who：参与事件的用户，distinct_id字段表示，未登录用户可以采用cookie、设备ID，已登录用户可以采用实际的用户ID。
- When：事件发生的时间，time字段表示，如果调用者不主动设置时间，则采用当前系统时间。
- Where：事件发生的地点，ip/city字段表示。
- How：触发事件的方式，方式主要涉及用户使用的设备、浏览器、App版本、操作系统版本、渠道等信息。
- What：时间的具体内容。

上述五点，最为关键的自然是What（事件的内容）。


除非某个行为只在前端发生，都后端没有任何请求，建议只在后端采集数据。

自动采集事件

App控件被点击时会发送$AppClick事件，包含被点击控件的相关信息，具体如下：

- $element_id - String 类型，为元素 ID，例如为 android：id 属性设置的值
- $element_type - String 类型，为元素类型，比如 Button、CheckBox 等
- $element_content - String 类型，为元素内容，比如是 Button、TextView 显示的文本
- $screen_name - String 类型，表示 Activity 的包名.类名
- $title - String 类型，表示 Activity 的标题

App启动或者从后台恢复时会发送$AppStart事件，包含启动相关信息，具体如下：

- $is_first_time - boolean 类型，true 表示 App 安装后首次启动，false 则相反
- $is_first_day - boolean 类型， ture 表示 App 安装后首日访问，false 则表示不是首日访问 （1.6.27 及以后版本支持该属性）
- $resume_from_background - boolean 类型，true 表示 App 从后台恢复，false 表示 App 启

App进入后台时发送$AppEnd事件，包含进入后台相关信息，具体如下：

- event_duration - long 类型，表示本次 App 启动的使用时长，单位为秒

App切换Activity时发送$AppViewScreen事件，记录页面切换相关信息，具体如下：

- $title - String 类型，表示 Activity 的标题（1.6.31 及以后版本支持该属性）。Android SDK 按照如下逻辑读取 title 属性：首先读取 activity.getTitle()，如果使用 actionBar，并且 actionBar.getTitle() 不为空，则actionBar.getTitle() 覆盖 activity.getTitle()，如果以上两步都没有读到 title，则获取 activity 的 android:label 属性。
- $screen_name - String 类型，表示 Activity 的包名.类名


事件预置属性

- $app_version	字符串	应用版本	应用的版本	
- $lib	字符串	SDK类型	例如 Android	
- $lib_version	字符串	SDK版本		
- $manufacturer	字符串	设备制造商	例如 Xiaomi	
- $model	字符串	设备型号	例如 Redmi 4X	
- $os	字符串	操作系统	例如 Android	
- $os_version	字符串	操作系统版本	例如 6.0.1	
- $screen_height	数值	屏幕高度	例如 1280	
- $screen_width	数值	屏幕宽度	例如 720	
- $wifi	BOOL	是否wifi		
- $carrier	字符串	运营商名称	例如 中国联通	
- $network_type	字符串	网络类型	例如 4G	
- $is_first_day	布尔值	是否首日访问		1.6.27支持
- $device_id	字符串	设备ID	获取值为 AndroidID

事件发送条件

- 是否是WIFI/2G/3G/4G网络条件
- 是否满足发送条件之一:
- 与上次发送的时间间隔是否大于 flushInterval
- 本地缓存日志数目是否大于 flushBulkSize


- com.guoxiaoxing.cuckoo.android.FlushInterval - 设置 SDK 的 flushInterval，单位毫秒，默认值为 15 秒；
- com.guoxiaoxing.cuckoo.android.FlushBulkSize - 设置 SDK 的 flushBulkSize，默认值为 100；
- com.guoxiaoxing.cuckoo.android.ResourcePackageName - 设置 App 的 Package Name，默认值为 Application 对象的 Package Name，当 App 的 R.* class 的 Package Name 与 Application不同时，需要手动填入该配置；
- com.guoxiaoxing.cuckoo.android.AndroidId - 1.6.40 及以后的版本支持将 Android ID 作为默认匿名 ID，"true" 表示使用 Android ID 作为 匿名 ID ，"false" 表示使用 Sensors Analytics SDK 随机分配一个唯一 ID（UUID）作为 匿名 ID ，默认值为 "false"；
- com.guoxiaoxing.cuckoo.android.ShowDebugInfoView- 1.6.40 及以后的版本支持设置 Toast ，"true" 表示 Debug 模式 下 出现错时显示 Toast 提示，"false" 表示不显示 Toast 提示，默认值为"true"；


事件上传流程

