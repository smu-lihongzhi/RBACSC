pragma solidity ^0.5.8;

contract IdentityManageContract {
    // 可信机构地址
    address public TA;
    
    // 累加器历史记录（时间戳 => 累加器值）
    mapping(uint256 => bytes32) public issueLedger;
    uint256[] public timestamps;
    
    // 存储每个实体的见证（公钥哈希 => 见证值）
    mapping(bytes32 => bytes32) public witnesses;
    
    // 存储每个实体的λ值（公钥哈希 => λ值）
    mapping(bytes32 => bytes32) public lambdas;
    
    // 系统参数
    bytes32 public g1;
    uint256 public currentAccumulatorIndex;
    
    // 事件记录
    event IdentityRegistered(bytes32 indexed entityKey, bytes32 lambda, bytes32 witness);
    event IdentityRevoked(bytes32 indexed revokedKey, bytes32 newAccumulator);
    event IdentityVerified(bytes32 accumulator);
    
    // 仅TA可调用的修饰器
    modifier onlyTA() {
        require(msg.sender == TA, "Only TA can call this function");
        _;
    }
    
    constructor(bytes32 _g1) public {
        TA = msg.sender;
        g1 = _g1;
        currentAccumulatorIndex = 0;
        
        // 初始化累加器（λ0）
        bytes32 lambda0 = keccak256(abi.encodePacked(uint256(1)));
        issueLedger[0] = keccak256(abi.encodePacked(g1, lambda0));
        timestamps.push(0);
    }
    
    // 身份注册函数
    function registerIdentity(
        bytes32 pubKey, 
        bytes32 sysID
    ) public onlyTA returns (bytes32, bytes32) {
        // 计算λ_i = H(Pubk_(E_i) || SysID_i)
        bytes32 lambda = keccak256(abi.encodePacked(pubKey, sysID));
        
        // 计算新累加器值: △_a_new = △_a * g1^λ_i
        bytes32 currentAccumulator = issueLedger[currentAccumulatorIndex];
        bytes32 newAccumulator = keccak256(abi.encodePacked(currentAccumulator, lambda));
        
        // 计算见证: W_i = △_a / g1^λ_i
        bytes32 witness = keccak256(abi.encodePacked(currentAccumulator, lambda, "div"));
        
        // 更新状态
        uint256 newTimestamp = now;
        currentAccumulatorIndex = timestamps.length;
        issueLedger[newTimestamp] = newAccumulator;
        timestamps.push(newTimestamp);
        
        // 存储实体数据
        bytes32 entityKey = keccak256(abi.encodePacked(pubKey));
        witnesses[entityKey] = witness;
        lambdas[entityKey] = lambda;
        
        emit IdentityRegistered(entityKey, lambda, witness);
        return (newAccumulator, witness);
    }
    
    // 身份验证函数
    function verifyIdentity() public view returns (bytes32) {
        require(timestamps.length > 0, "No identities registered");
        return issueLedger[timestamps[timestamps.length - 1]];
    }
    
    // 身份撤销函数
    function revokeIdentity(
        bytes32 pubKey, 
        bytes32 sysID
    ) public onlyTA returns (bytes32[] memory) {
        bytes32 entityKey = keccak256(abi.encodePacked(pubKey));
        bytes32 lambda = lambdas[entityKey];
        
        // 获取当前累加器
        bytes32 currentAccumulator = issueLedger[timestamps[timestamps.length - 1]];
        
        // 计算新累加器: △_a_new = △_a / g1^λ_i
        bytes32 newAccumulator = keccak256(abi.encodePacked(currentAccumulator, lambda, "div"));
        
        // 更新累加器历史
        uint256 newTimestamp = now;
        issueLedger[newTimestamp] = newAccumulator;
        timestamps.push(newTimestamp);
        
        // 更新见证列表（伪代码实现，实际需链下计算）
        bytes32[] memory updatedWitnesses;
        // 实际实现中需要遍历所有见证并更新
        
        // 清除被撤销的身份
        delete witnesses[entityKey];
        delete lambdas[entityKey];
        
        emit IdentityRevoked(entityKey, newAccumulator);
        return updatedWitnesses;
    }
    
    // 获取最新累加器值
    function getLatestAccumulator() public view returns (bytes32) {
        return issueLedger[timestamps[timestamps.length - 1]];
    }
    
    // 获取实体见证
    function getWitness(bytes32 pubKey) public view returns (bytes32) {
        return witnesses[keccak256(abi.encodePacked(pubKey))];
    }
}