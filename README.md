# Counter

## Write consistency

- `WriteLocal`: すぐにローカルレプリカにのみ書き込み、後でゴシップ配布される
- `WriteTo(n)`: ローカルレプリカを含む少くともn個のレプリカにすぐに書き込まれる
- `WriteMajoriy`: 大部分のレプリカ、つまり、少なくとも N/2+1 のレプリカにすぐに書き込まれる。Nはクラスター(または cluster role group)内のノード数。
- `WriteMajorityPlus`: `WriteMajority`に似ているが、指定された数の追加ノードが多数決により追加される。最大で全てのノードとなる。これにより書き込みと読み取りの間のメンバーシップの変更に対する許容度が向上する。
- `WriteAll`: クラスター内の全てのノード(または cluster role group内のすべてのノード)にすぐに書き込まれる

## Read consistency

- `ReadLocal`: ローカルレプリカからのみ読み取られる
- `ReadFrom(n)`: ローカルレプリカを含むn個のレプリカから値が読み取られ、マージされる
- `ReadMajority`: 過半数のレプリカ、つまり少なくとも N/2+1 のレプリカから読み取られてマージされる。Nはクラスター(または cluster role group)内のノード数。
- `ReadMajorityPlus`: `ReadMajority`に似ているが、指定された数の追加ノードが多数決により追加される。最大で全てのノードとなる。これにより書き込みと読み取りの間のメンバーシップの変更に対する許容度が向上する。
- `ReadAll`: クラスター内の全てのノード(または cluster role group内のすべてのノード)から読み取られてマージされる

(注意) `ReadMajority`および`ReadMajorityPlus`には、小さなクラスターの安全性を高めるために指定するのに役立つ`minCap`パラメーターがある。
