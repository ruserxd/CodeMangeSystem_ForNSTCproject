import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { Tree, Typography } from 'antd';
import { FileOutlined, CodeOutlined } from '@ant-design/icons';
import api from '../../api/axiosConfig';
import HighlightedCode from './HighlightedCode';

const { Title } = Typography;

const ShowMethodDiff = () => {
	const { '*': urlParam } = useParams();
	const [treeData, setTreeData] = useState([]);
	const [projectName, setProjectName] = useState('');
	const [error, setError] = useState(null);

	useEffect(() => {
		const fetchData = async () => {
			try {
				const result = await api.post(
					`/api/getData`,
					new URLSearchParams({ ProjectName: urlParam })
				);
				setTreeData(transformData(result.data));
				setProjectName(urlParam.substring(urlParam.lastIndexOf('/') + 1));
			} catch (error) {
				setError(error);
				console.error('Error during fetch: ', error);
			}
		};

		if (urlParam) {
			fetchData();
		}
	}, [urlParam]);

	const transformData = (data) => {
		return data.map((item) => ({
			title: item.fileName,
			key: item.filePath,
			icon: <FileOutlined />,
			children: item.methods.map((method, methodIndex) => ({
				title: method.methodName,
				key: `${item.filePath}-${methodIndex}`,
				icon: <CodeOutlined />,
				children: method.diffInfoList.map((diff, diffIndex) => ({
					title: `Diff ${diffIndex + 1}`,
					key: `${item.filePath}-${methodIndex}-${diffIndex}`,
					icon: <CodeOutlined />,
					diffInfo: diff
				}))
			}))
		}));
	};

	const renderDiffInfo = (diffInfo) => (
		<div className="diffINFO">
			<Title level={4}>Author: {diffInfo.author}</Title>
			<Title level={4}>Author Email: {diffInfo.authorEmail}</Title>
			<Title level={4}>Commit Message: {diffInfo.commitMessage}</Title>
			<Title level={4}>Commit Time: {diffInfo.commitTime}</Title>
			<HighlightedCode language="diff" codeString={diffInfo.diffCode} className="diffCode" />
		</div>
	);

	if (error) {
		return <div>Error: {error.message}</div>;
	}

	return (
		<div>
			<Title level={2}>{projectName} 的方法差異資訊</Title>
			<Tree
				treeData={treeData}
				defaultExpandAll
				showIcon
				titleRender={(nodeData) => {
					if (nodeData.diffInfo) {
						return (
							<div>
								<span>{nodeData.title}</span>
								{renderDiffInfo(nodeData.diffInfo)}
							</div>
						);
					}
					return <span>{nodeData.title}</span>;
				}}
			/>
		</div>
	);
};

export default ShowMethodDiff;
