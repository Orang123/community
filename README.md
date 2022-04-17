# community
仿牛客论坛 springboot+redis+kafka+es+security+quartz+caffeine
技术栈采用springboot+springmvc+mybatis+redis+kafka+elasticsearch+spring security+quartz+Caffeine+qiniu.
实现的功能包括发布帖子，对帖子内容用trie树进行敏感词过滤，对帖子加精、置顶，对帖子进行分词应用es进行搜索，
发布评论回复，用户私信、系统通知，采用redis缓存对帖子、评论的点赞数，应用redis存放用户之间的关注信息，
redis缓存登陆验证码、登陆凭证、用户信息，登陆用户信息，以及帖子id方便定时更新帖子分数，采用采用threadlocal存放实现线程隔离，
应用redis HyperLogLog 统计网站独立访客(uv),bitmap统计日活跃用户(dau)，采用消息队列kafka延迟存储用户系统通知信息、将添加帖子、
帖子信息改变时延缓同步es索引数据库。采用quartz定时任务更新数据库、es中的帖子分数，方便用户根据帖子分数搜索排名热帖，采用本地缓存
Caffeine缓存热帖，降低数据库访问压力，提高热帖数据访问性能。
