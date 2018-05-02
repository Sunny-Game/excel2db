@echo off
set language=csharp
set beanRoot=X:\zhanWuShuang_project\ClientPart\zhanWuShuang_tv_project\Assets\Scripts\GameLogic\Data\PrototypeDeserialize\Bean
set nameSpace=
set excelPath=..\excel
set ndbPath=X:\zhanWuShuang_project\ClientPart\zhanWuShuang_tv_project\Assets\Resources\Art\Data\package_data_prototype

java -jar excel2db-write-0.0.1-SNAPSHOT\excel2db-write-0.0.1-SNAPSHOT.jar %language% %beanRoot% %nameSpace% %excelPath% %ndbPath%

pause