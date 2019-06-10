ES match_phrase查询原理

match_pharse是词组查询，首先对 匹配短语 进行分析，拆解成一个个的term：

第一步：找到包含匹配短语中所有term的所有文档

第二步 ：去掉那些不连续有序的文档。

