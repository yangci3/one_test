# �׶� P4���� ICMP ̽�⣨mock / icmp ���л���

**���ڣ�** 2026-08-13  
**״̬��** �����ȷ�ϣ���дʵ�ּƻ�  
**·��ͼ��** `docs/superpowers/specs/2026-07-31-factory-scene-network-roadmap.md`  
**������** P1 ���ģ��̽�⣻P2 ��ʷ���  
**������** `docs/superpowers/specs/2026-08-01-phase-p1-backend-mock-probe-design.md`

## 1. Ŀ��

�ڱ������� mock ��ת��ǰ���£����ӻ����豸 IP ����ʵ ICMP ̽�⣻ͨ������������ģʽ���л���Ĭ�� mock�������޳�����ʱ�ù��� IP���� 8.8.8.8��������

**��ز��ԣ����� A�����Խӿ� + �����л���**

**���ڱ�׼**

1. `scene.probe.mode=mock` ʱ��Ϊ�� P1 һ�£����ʷ�ת + ��ʷ��
2. `mode=icmp` ʱ�ԺϷ� IP ִ�� ping������ 3 ��ʧ�� �� offline��1 �γɹ� �� online
3. ��/�Ƿ� IP���� tick ��������ǿ�Ƹ�д status������ unknown ��������
4. ״̬ʵ�ʱ仯ʱ��д�� `scene_probe_event`������ P2��
5. �����ò�������˼����л���ǰ��̽�� API / �澯 UI ����Ϊ���

**����**

- ���������л� mode
- SNMP���׶� F��
- ������ tiles��P3��
- ���û� `probeIntervalMs` ��д��˵�������

## 2. ����ժҪ

| �� | ѡ�� |
| --- | --- |
| �ܹ� | ���Խӿ� `SceneProbeReachability`��Mock / Icmp ��ʵ�� |
| Ĭ��ģʽ | `mock` |
| �л���ʽ | `application.yml` �� `scene.probe.mode`��������Ч |
| Offline | ����ʧ�� �� 3������ `icmp.fail-threshold`�� |
| Online �ָ� | �����ɹ� �� 1������ `icmp.recover-threshold`�� |
| ��Ч IP | ���� ping����ǿ�Ƹ� status |
| ICMP ʵ�� | Windows��`ping -n 1 -w <timeoutMs> <ip>` |
| ʧ�ܼ��� | �������ڴ棻�������㣻���½��� |
| ���� | ���ù��� IP���س�������豸 IP ���� |

## 3. �ܹ�

������

- `SceneProbeScheduler` �� `ISceneProbeService.tick()`
- `scene_probe_state` / start��stop / P2 ��ʷд����ü�

������

- `SceneProbeReachability`����ȼ�������
  - `MockProbeReachability`������ offline-prob / recover-prob �߼�Ǩ��
  - `IcmpProbeReachability`��ִ��ϵͳ ping������ success/fail
- `tick()` �� `mode` ѡ����ԣ�icmp ��ά�� per-device �����ɹ�/ʧ�ܼ������پ����Ƿ�� status

ǰ�ˣ�������ѯ `/scene/probe/list`��������̽��ģʽ API�����׶Σ���

## 4. ����

�� `scene.probe` �£�

| �� | Ĭ�� | ˵�� |
| --- | --- | --- |
| `mode` | `mock` | `mock` �� `icmp` |
| `interval-ms` | `5000` | ���ȼ�������У� |
| `offline-prob` | `0.15` | �� mock |
| `recover-prob` | `0.40` | �� mock |
| `icmp.timeout-ms` | `2000` | ���� ping ��ʱ�����룬ӳ�䵽 ping -w�� |
| `icmp.fail-threshold` | `3` | ����ʧ�� �� offline |
| `icmp.recover-threshold` | `1` | �����ɹ� �� online |

�Ƿ� `mode` ֵ���������״� tick ʱ���� `mock` ������־��ʵ�ּƻ���д������

## 5. ICMP ��״̬��

### 5.1 ִ��

- ������̬��Windows����`ping -n 1 -w <timeoutMs> <ip>`
- ���ݽ����˳�����/������ж�ͨ��
- ͬһ tick �ڶԶ��豸�����޲������У�Ĭ�ϴ��л�С�������ޣ�����������̣���ʵ�ּƻ�ѡ��һ�ֲ�д����ע���

### 5.2 ����mode=icmp �� monitoring=1��

| ���� | ��Ϊ |
| --- | --- |
| IP �ջ�Ƿ� | �� ping���� tick ���� status |
| ping �ɹ� | ʧ�ܼ���=0���ɹ�����+1�����ɹ����� �� recover-threshold �� status �� online �� online + ��ʷ |
| ping ʧ�� | �ɹ�����=0��ʧ�ܼ���+1����ʧ�ܼ��� �� fail-threshold �� status �� offline �� offline + ��ʷ |
| start | ������һ�£�monitoring + ���� online ��д online �¼� |
| stop | unknown����д��ʷ |

�� status **ʵ�ʱ仯** ʱ�������� `recordEvent` + trim��

## 6. ����

1. `mode=mock`�������ת����ʷ������  
2. `mode=icmp` + IP=`8.8.8.8`���� `1.1.1.1`������ online  
3. IP ��Ϊ���ɴԼ 3 ���������ں� offline����ʷ�м�¼  
4. IP ��գ�����̽������  
5. �Ļ� `mock` ���������ָ�ģ��  

## 7. ����

| ���� | ���� |
| --- | --- |
| ������ ICMP / ���� | �ĵ�˵��ѡ���ù��� DNS����ֵ���� |
| Windows ping �������� | ���˳���Ϊ�������Ϊ�����ֲ��嵥 |
| tick ���豸������������ | ���޲���/���У�interval ���� |
| ���� icmp �������´���� offline | Ĭ�� mock����ֵ |

## 8. ����

- [x] �� TBD  
- [x] ����ȷ��ѡ��һ�£����� A��Ĭ�� mock��N=3 / �ָ� 1����Ч IP ������  
- [x] ���� P3 / SNMP  
- [x] �� P1/P2 ��������ʷ·������  
